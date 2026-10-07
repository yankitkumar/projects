#!/usr/bin/env bash
# End-to-end smoke test for ticket-service + ai-service, run against real running services.
#
#   scripts/smoke-test.sh                 both services up: full API walkthrough
#   scripts/smoke-test.sh --with-outage   ai-service stopped, ticket-service up: checks that
#                                         tickets are still created, just without AI insights
#
# Override the targets with TICKET_URL and AI_URL. Needs only curl and python3.
# Prints one PASS line per check and exits non-zero on the first failure.
set -euo pipefail

BASE=${TICKET_URL:-http://localhost:8081}
AI=${AI_URL:-http://localhost:8082}
WAIT_SECONDS=${SMOKE_WAIT_SECONDS:-60}

MODE=full
case "${1:-}" in
    "") ;;
    --with-outage) MODE=outage ;;
    -h|--help) sed -n '2,9p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "unknown argument: $1 (try --help)" >&2; exit 2 ;;
esac

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
STATUS=
PASSED=0

pass() {
    PASSED=$((PASSED + 1))
    printf 'PASS  %s\n' "$1"
}

fail() {
    printf 'FAIL  %s\n' "$1" >&2
    if [[ -s $WORK/body ]]; then
        printf '      last response (HTTP %s): %s\n' "$STATUS" "$(head -c 600 "$WORK/body")" >&2
    fi
    exit 1
}

# request METHOD URL [JSON] -> sets STATUS; body and headers land in $WORK.
request() {
    local method=$1 url=$2 data=${3:-}
    local args=(-sS -m 120 -o "$WORK/body" -D "$WORK/headers" -w '%{http_code}' -X "$method")
    if [[ -n $data ]]; then
        args+=(-H 'Content-Type: application/json' --data "$data")
    fi
    : > "$WORK/body"
    STATUS=$(curl "${args[@]}" "$url") || fail "$method $url: could not connect"
}

# The Python helpers every JSON assertion can use; `body` is the parsed last response.
# Expressions are wrapped in parentheses before eval, so they may span several lines.
PRELUDE='
import json, sys
with open(sys.argv[1], encoding="utf-8") as f:
    body = json.load(f)
RANK = {"URGENT": 0, "HIGH": 1, "MEDIUM": 2, "LOW": 3}
def rank(ticket):
    return RANK.get((ticket.get("ai") or {}).get("priority"), len(RANK))
def ids():
    return [t["id"] for t in body]
'

# json_value EXPR -> prints the expression evaluated against the last response body.
json_value() {
    python3 -I -c "$PRELUDE
print(eval('(' + sys.argv[2] + ')'))" "$WORK/body" "$1" 2>/dev/null
}

# check DESCRIPTION EXPECTED_STATUS [PYTHON_EXPR]; without an expression only the status is checked.
check() {
    local description=$1 expected=$2 expr=${3:-}
    [[ $STATUS == "$expected" ]] || fail "$description (expected HTTP $expected, got $STATUS)"
    if [[ -n $expr ]]; then
        python3 -I -c "$PRELUDE
sys.exit(0 if eval('(' + sys.argv[2] + ')') else 1)" "$WORK/body" "$expr" 2>/dev/null \
            || fail "$description (assertion failed: $expr)"
    fi
    pass "$description"
}

header() {
    awk -v name="$(tr '[:upper:]' '[:lower:]' <<< "$1"):" 'tolower($1) == name { print $2 }' "$WORK/headers" | tr -d '\r'
}

wait_for_health() {
    local name=$1 url=$2
    for ((i = 0; i < WAIT_SECONDS; i++)); do
        if curl -s -m 2 "$url/actuator/health" 2>/dev/null | grep -q '"UP"'; then
            pass "$name is UP ($url/actuator/health)"
            return
        fi
        sleep 1
    done
    fail "$name did not report UP at $url/actuator/health within ${WAIT_SECONDS}s"
}

# create_ticket SUBJECT BODY EMAIL -> sets STATUS and TICKET_ID.
create_ticket() {
    local payload
    payload=$(python3 -I -c 'import json, sys; print(json.dumps({"subject": sys.argv[1], "body": sys.argv[2], "customerEmail": sys.argv[3]}))' "$@")
    request POST "$BASE/api/tickets" "$payload"
    TICKET_ID=$(json_value 'body["id"]' || true)
    [[ $TICKET_ID =~ ^[0-9]+$ ]] || fail "POST /api/tickets did not return a numeric id"
    [[ $(header Location) == "/api/tickets/$TICKET_ID" ]] \
        || fail "POST /api/tickets: Location header was '$(header Location)', expected /api/tickets/$TICKET_ID"
}

run_outage_checks() {
    if curl -s -m 3 -o /dev/null "$AI/actuator/health" 2>/dev/null; then
        fail "ai-service is still answering at $AI; stop it before running --with-outage"
    fi
    pass "ai-service is down ($AI is unreachable)"
    wait_for_health ticket-service "$BASE"

    create_ticket "Cannot log in to my account" \
        "I reset my password but I am still unable to log in. Please help." "sam@example.com"
    local id=$TICKET_ID
    check "create ticket while ai-service is down -> 201, aiStatus UNAVAILABLE, no insights" 201 \
        'body["aiStatus"] == "UNAVAILABLE" and body["ai"] is None and body["status"] == "OPEN"'

    request GET "$BASE/api/tickets/$id"
    check "GET /api/tickets/$id -> stored as UNAVAILABLE" 200 'body["aiStatus"] == "UNAVAILABLE"'

    request POST "$BASE/api/tickets/$id/analyze"
    check "re-analyze while ai-service is down -> 200, still UNAVAILABLE" 200 \
        'body["aiStatus"] == "UNAVAILABLE" and body["ai"] is None'

    request GET "$BASE/api/tickets/stats"
    check "GET /api/tickets/stats counts the ticket as unanalyzed" 200 'body["unanalyzed"] >= 1'
}

run_full_checks() {
    wait_for_health ai-service "$AI"
    wait_for_health ticket-service "$BASE"

    request GET "$AI/api/ai/info"
    check "GET /api/ai/info -> engine and model are consistent" 200 \
        '(body["engine"] == "rules" and body["model"] is None) or (body["engine"] == "claude" and bool(body["model"]))'
    local engine
    engine=$(json_value 'body["engine"]') || fail "could not read engine from /api/ai/info"
    echo "      ai-service engine: $engine"

    request POST "$AI/api/ai/analyze" '{"subject": "Invoice question", "body": "Why was my invoice higher this month?"}'
    check "POST /api/ai/analyze -> 200 with the contract's enum values" 200 '
        body["category"] in ("BILLING", "TECHNICAL", "ACCOUNT", "SHIPPING", "FEEDBACK", "OTHER")
        and body["priority"] in ("LOW", "MEDIUM", "HIGH", "URGENT")
        and body["sentiment"] in ("POSITIVE", "NEUTRAL", "NEGATIVE")
        and body["summary"] and body["suggestedReply"] and body["engine"] in ("claude", "rules")'

    request POST "$AI/api/ai/analyze" '{"subject": "", "body": ""}'
    check "POST /api/ai/analyze with blank fields -> 400 field map" 400 \
        'body.get("subject") and body.get("body")'

    create_ticket "Production down: checkout fails for all users" \
        "Since 09:10 UTC every checkout request fails with a 500 error. This is an outage affecting all users, please help immediately." \
        "ops@example.com"
    local outage=$TICKET_ID
    check "create outage ticket -> 201, ANALYZED as TECHNICAL / URGENT" 201 \
        'body["aiStatus"] == "ANALYZED" and body["status"] == "OPEN"
         and body["ai"]["category"] == "TECHNICAL" and body["ai"]["priority"] == "URGENT"'

    create_ticket "Charged twice for my subscription" \
        "I was charged twice for my October subscription invoice and I am really frustrated. Please refund the duplicate payment." \
        "dana@example.com"
    local billing=$TICKET_ID
    check "create double-charge ticket -> 201, ANALYZED as BILLING / HIGH / NEGATIVE" 201 \
        'body["aiStatus"] == "ANALYZED" and body["ai"]["category"] == "BILLING"
         and body["ai"]["priority"] == "HIGH" and body["ai"]["sentiment"] == "NEGATIVE"'

    create_ticket "Feature suggestion: dark mode" \
        "I love the new dashboard, thanks! A small suggestion: a dark mode would be great for late-night work." \
        "lee@example.com"
    local feedback=$TICKET_ID
    check "create feature-suggestion ticket -> 201, ANALYZED as FEEDBACK / LOW / POSITIVE" 201 \
        'body["aiStatus"] == "ANALYZED" and body["ai"]["category"] == "FEEDBACK"
         and body["ai"]["priority"] == "LOW" and body["ai"]["sentiment"] == "POSITIVE"'

    request GET "$BASE/api/tickets"
    check "GET /api/tickets -> all three listed, sorted by priority (URGENT first), then id" 200 "
        {$outage, $billing, $feedback} <= set(ids())
        and [(rank(t), t['id']) for t in body] == sorted((rank(t), t['id']) for t in body)
        and rank(body[0]) == 0"

    request GET "$BASE/api/tickets?category=billing"
    check "GET /api/tickets?category=billing -> only BILLING tickets (case-insensitive)" 200 "
        $billing in ids() and $outage not in ids()
        and all(t['ai']['category'] == 'BILLING' for t in body)"

    request GET "$BASE/api/tickets?priority=URGENT"
    check "GET /api/tickets?priority=URGENT -> only URGENT tickets" 200 "
        $outage in ids() and all(t['ai']['priority'] == 'URGENT' for t in body)"

    request GET "$BASE/api/tickets/$billing"
    check "GET /api/tickets/$billing -> the double-charge ticket" 200 \
        "body['id'] == $billing and body['subject'] == 'Charged twice for my subscription'"

    request GET "$BASE/api/tickets/stats"
    local stats_ok='body["total"] >= 3 and body["open"] + body["resolved"] == body["total"]
        and body["byCategory"].get("TECHNICAL", 0) >= 1 and body["byCategory"].get("BILLING", 0) >= 1
        and body["byCategory"].get("FEEDBACK", 0) >= 1 and body["byPriority"].get("URGENT", 0) >= 1'
    check "GET /api/tickets/stats -> totals add up and include the new categories" 200 "$stats_ok"

    request POST "$BASE/api/tickets/$billing/analyze"
    check "POST /api/tickets/$billing/analyze -> 200, re-analyzed as BILLING" 200 \
        "body['id'] == $billing and body['aiStatus'] == 'ANALYZED' and body['ai']['category'] == 'BILLING'"

    request POST "$BASE/api/tickets/$outage/resolve"
    check "POST /api/tickets/$outage/resolve -> 200, status RESOLVED" 200 "body['status'] == 'RESOLVED'"

    request GET "$BASE/api/tickets?status=resolved"
    check "GET /api/tickets?status=resolved -> includes the resolved ticket" 200 "
        $outage in ids() and all(t['status'] == 'RESOLVED' for t in body)"

    request DELETE "$BASE/api/tickets/$feedback"
    check "DELETE /api/tickets/$feedback -> 204" 204

    request GET "$BASE/api/tickets/$feedback"
    check "GET /api/tickets/$feedback after delete -> 404" 404

    request POST "$BASE/api/tickets" '{"subject": "", "body": "", "customerEmail": "not-an-email"}'
    check "POST /api/tickets with invalid fields -> 400 field map" 400 '
        body.get("subject") == "subject must not be blank" and body.get("body") == "body must not be blank"
        and "customerEmail" in body'

    request GET "$BASE/api/tickets/not-a-number"
    check "GET /api/tickets/not-a-number -> 400" 400 '"error" in body'
}

echo "Smoke test ($MODE): ticket-service=$BASE ai-service=$AI"
if [[ $MODE == outage ]]; then
    run_outage_checks
else
    run_full_checks
fi
echo "All $PASSED checks passed."
