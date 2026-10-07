package com.example.aiservice.analysis;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic keyword-based triage. Used when no API key is configured and as the fallback
 * whenever the Claude call fails, so it must never throw for valid input.
 */
@Component
public class RuleBasedAnalyzer implements TicketAnalyzer {

    static final int SUMMARY_MAX_LENGTH = 140;

    // EnumMap iterates in enum order, which is what breaks ties between categories.
    private static final Map<Category, List<Pattern>> CATEGORY_KEYWORDS = new EnumMap<>(Map.of(
            Category.BILLING, patterns("invoice", "charge", "charged", "refund", "payment", "billing", "price",
                    "subscription"),
            Category.TECHNICAL, patterns("error", "crash", "bug", "broken", "not working", "down", "outage", "slow",
                    "timeout"),
            Category.ACCOUNT, patterns("password", "login", "log in", "sign in", "account", "locked", "2fa"),
            Category.SHIPPING, patterns("delivery", "shipping", "package", "tracking", "courier", "arrived"),
            Category.FEEDBACK, patterns("suggestion", "feature request", "feedback", "love", "great")));

    private static final List<Pattern> URGENT_PHRASES = patterns("urgent", "asap", "immediately", "outage",
            "production down", "all users", "security", "data loss");

    private static final List<Pattern> BLOCKED_PHRASES = patterns("can't", "cannot", "unable", "locked out",
            "charged twice", "not working", "broken", "error");

    private static final List<Pattern> NEGATIVE_WORDS = patterns("angry", "frustrated", "terrible", "awful", "worst",
            "unacceptable", "disappointed", "annoyed", "ridiculous");

    private static final List<Pattern> POSITIVE_WORDS = patterns("thanks", "thank you", "great", "love", "appreciate",
            "awesome", "happy");

    private static final Map<Category, String> REPLIES = new EnumMap<>(Map.of(
            Category.BILLING, "Thank you for contacting us about your billing question. "
                    + "Our billing team will review your account and the charges you mentioned, "
                    + "and we will follow up with the next steps.",
            Category.TECHNICAL, "Thank you for reporting this problem, and sorry for the trouble it is causing. "
                    + "Our technical team is looking into it. "
                    + "Any error messages, screenshots or steps to reproduce the issue will help us investigate.",
            Category.ACCOUNT, "Thank you for getting in touch about your account. "
                    + "For your security, please never share your password with us. "
                    + "Our support team will help you regain access and will reply here with the next steps.",
            Category.SHIPPING, "Thank you for contacting us about your order. "
                    + "We are checking the delivery status with our shipping team "
                    + "and will update you as soon as we have more information.",
            Category.FEEDBACK, "Thank you for taking the time to share your feedback. "
                    + "We have passed it on to our product team, who read every suggestion we receive.",
            Category.OTHER, "Thank you for contacting support. "
                    + "We have received your message and a member of our team will review it and get back to you."));

    @Override
    public TicketAnalysis analyze(String subject, String body) {
        // Curly apostrophes from phones and word processors would otherwise miss "can't".
        String text = (subject + "\n" + body).replace('’', '\'');
        Category category = categorize(text);
        return new TicketAnalysis(
                category,
                prioritize(text, category),
                sentiment(text),
                summarize(body),
                REPLIES.get(category));
    }

    private static Category categorize(String text) {
        Category best = Category.OTHER;
        int bestHits = 0;
        for (Map.Entry<Category, List<Pattern>> entry : CATEGORY_KEYWORDS.entrySet()) {
            int hits = countHits(text, entry.getValue());
            if (hits > bestHits) {
                best = entry.getKey();
                bestHits = hits;
            }
        }
        return best;
    }

    private static Priority prioritize(String text, Category category) {
        if (countHits(text, URGENT_PHRASES) > 0) {
            return Priority.URGENT;
        }
        if (countHits(text, BLOCKED_PHRASES) > 0) {
            return Priority.HIGH;
        }
        if (category == Category.FEEDBACK || category == Category.OTHER) {
            return Priority.LOW;
        }
        return Priority.MEDIUM;
    }

    private static Sentiment sentiment(String text) {
        int negative = countHits(text, NEGATIVE_WORDS);
        int positive = countHits(text, POSITIVE_WORDS);
        if (negative > positive) {
            return Sentiment.NEGATIVE;
        }
        if (positive > negative) {
            return Sentiment.POSITIVE;
        }
        return Sentiment.NEUTRAL;
    }

    static String summarize(String body) {
        String sentence = firstSentence(body.strip());
        if (sentence.length() <= SUMMARY_MAX_LENGTH) {
            return sentence;
        }
        return sentence.substring(0, SUMMARY_MAX_LENGTH - 3).stripTrailing() + "...";
    }

    private static String firstSentence(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r') {
                return text.substring(0, i).strip();
            }
            boolean endsSentence = c == '.' || c == '!' || c == '?';
            if (endsSentence && (i + 1 == text.length() || Character.isWhitespace(text.charAt(i + 1)))) {
                return text.substring(0, i + 1);
            }
        }
        return text;
    }

    private static int countHits(String text, List<Pattern> patterns) {
        int hits = 0;
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                hits++;
            }
        }
        return hits;
    }

    // Word boundaries on both sides, so "down" does not match inside "download".
    private static List<Pattern> patterns(String... phrases) {
        return Arrays.stream(phrases)
                .map(phrase -> Pattern.compile("\\b" + Pattern.quote(phrase) + "\\b",
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE))
                .toList();
    }
}
