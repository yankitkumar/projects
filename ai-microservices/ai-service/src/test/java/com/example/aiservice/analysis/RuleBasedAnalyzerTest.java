package com.example.aiservice.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedAnalyzerTest {

    private final RuleBasedAnalyzer analyzer = new RuleBasedAnalyzer();

    @ParameterizedTest(name = "{2}: {0}")
    @CsvSource(delimiter = '|', textBlock = """
            Question about my invoice | The price on my subscription invoice looks different this month. | BILLING
            App keeps timing out      | Every report hits a timeout and the dashboard is slow.          | TECHNICAL
            Password change           | How do I change the password on my account?                     | ACCOUNT
            Where is my package?      | The tracking page says my package has not arrived yet.          | SHIPPING
            Feature request           | A dark mode would be nice. Just a suggestion.                   | FEEDBACK
            Office hours              | What time does your office open on Saturdays?                   | OTHER
            """)
    void categorizesByKeywordHits(String subject, String body, Category expected) {
        assertThat(analyzer.analyze(subject, body).category()).isEqualTo(expected);
    }

    @Test
    void tiesGoToTheEarlierCategory() {
        // One BILLING hit ("payment") and one TECHNICAL hit ("error").
        TicketAnalysis analysis = analyzer.analyze("Checkout", "My payment page shows an error.");

        assertThat(analysis.category()).isEqualTo(Category.BILLING);
    }

    @Test
    void matchesWholeWordsOnly() {
        // "download" must not count as "down", and "accountant" must not count as "account".
        TicketAnalysis analysis = analyzer.analyze("Download link", "Where can I download the accountant guide?");

        assertThat(analysis.category()).isEqualTo(Category.OTHER);
        assertThat(analysis.priority()).isEqualTo(Priority.LOW);
    }

    @Test
    void matchingIsCaseInsensitive() {
        TicketAnalysis analysis = analyzer.analyze("REFUND", "I would like a REFUND for my last PAYMENT.");

        assertThat(analysis.category()).isEqualTo(Category.BILLING);
    }

    @Test
    void urgentForOutagesSecurityAndDataLoss() {
        assertThat(analyzer.analyze("Outage", "Nothing loads for anyone.").priority()).isEqualTo(Priority.URGENT);
        assertThat(analyzer.analyze("Help", "Production down since 9am.").priority()).isEqualTo(Priority.URGENT);
        assertThat(analyzer.analyze("Account", "Possible security issue with my login.").priority())
                .isEqualTo(Priority.URGENT);
        assertThat(analyzer.analyze("Sync", "We had data loss after the sync.").priority())
                .isEqualTo(Priority.URGENT);
    }

    @Test
    void urgentWinsOverBlocked() {
        TicketAnalysis analysis = analyzer.analyze("Error on checkout", "Please fix this ASAP, we cannot take orders.");

        assertThat(analysis.priority()).isEqualTo(Priority.URGENT);
    }

    @Test
    void highWhenCustomerIsBlocked() {
        assertThat(analyzer.analyze("Login", "I can't log in to my account.").priority()).isEqualTo(Priority.HIGH);
        assertThat(analyzer.analyze("Billing", "I was charged twice for May.").priority()).isEqualTo(Priority.HIGH);
        assertThat(analyzer.analyze("Locked out", "I am locked out after too many tries.").priority())
                .isEqualTo(Priority.HIGH);
    }

    @Test
    void curlyApostropheStillCountsAsBlocked() {
        TicketAnalysis analysis = analyzer.analyze("Login", "I can’t sign in since yesterday.");

        assertThat(analysis.category()).isEqualTo(Category.ACCOUNT);
        assertThat(analysis.priority()).isEqualTo(Priority.HIGH);
    }

    @Test
    void lowForFeedbackAndOther() {
        assertThat(analyzer.analyze("Feedback", "Some feedback on the new editor.").priority())
                .isEqualTo(Priority.LOW);
        assertThat(analyzer.analyze("Hi", "Do you have a student discount?").priority()).isEqualTo(Priority.LOW);
    }

    @Test
    void mediumOtherwise() {
        TicketAnalysis analysis = analyzer.analyze("Invoice", "Can you resend my invoice for April?");

        assertThat(analysis.category()).isEqualTo(Category.BILLING);
        assertThat(analysis.priority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    void negativeSentiment() {
        TicketAnalysis analysis = analyzer.analyze("Refund", "This is unacceptable and I am frustrated.");

        assertThat(analysis.sentiment()).isEqualTo(Sentiment.NEGATIVE);
    }

    @Test
    void positiveSentiment() {
        TicketAnalysis analysis = analyzer.analyze("Thank you", "I love the new dashboard, great work.");

        assertThat(analysis.sentiment()).isEqualTo(Sentiment.POSITIVE);
    }

    @Test
    void neutralWhenBalancedOrNoSentimentWords() {
        assertThat(analyzer.analyze("Thanks", "But the export is terrible.").sentiment()).isEqualTo(Sentiment.NEUTRAL);
        assertThat(analyzer.analyze("Invoice", "Please resend my invoice.").sentiment()).isEqualTo(Sentiment.NEUTRAL);
    }

    @Test
    void summaryIsTheFirstSentenceOfTheBody() {
        TicketAnalysis analysis = analyzer.analyze("Crash", "  The app crashed on startup. I lost my draft!  ");

        assertThat(analysis.summary()).isEqualTo("The app crashed on startup.");
    }

    @Test
    void summaryStopsAtTheFirstLineBreak() {
        assertThat(RuleBasedAnalyzer.summarize("Order 1234\nIt has not arrived")).isEqualTo("Order 1234");
    }

    @Test
    void longSummaryIsTruncatedWithEllipsis() {
        String body = "word ".repeat(60) + "end.";

        String summary = RuleBasedAnalyzer.summarize(body);

        assertThat(summary).hasSizeLessThanOrEqualTo(RuleBasedAnalyzer.SUMMARY_MAX_LENGTH).endsWith("...");
        assertThat(summary).startsWith("word word");
    }

    @Test
    void replyMatchesTheCategoryWithoutPromises() {
        TicketAnalysis analysis = analyzer.analyze("Refund", "I want a refund for my subscription.");

        assertThat(analysis.suggestedReply()).contains("billing").doesNotContainIgnoringCase("refund");
    }
}
