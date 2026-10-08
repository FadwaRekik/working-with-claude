package com.marlowefinch.ops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** The JSON API through MockMvc, plus one real HTTP call to observe the error path. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class DashboardControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestRestTemplate http;

    @Test
    void healthReportsUpAndTheFixedToday() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.today").value("2026-09-21"));
    }

    @Test
    void kpisDefaultToTheLast30DaysEndingToday() throws Exception {
        mvc.perform(get("/api/kpis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-08-22"))
                .andExpect(jsonPath("$.to").value("2026-09-21"))
                .andExpect(jsonPath("$.onTimeRate").value(0.937))
                .andExpect(jsonPath("$.openTickets").value(114))
                .andExpect(jsonPath("$.revenue").value(360095.5))
                .andExpect(jsonPath("$.orders").value(624));
    }

    @Test
    void kpisAcceptAnExplicitRange() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "2026-07-01").param("to", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-07-01"))
                .andExpect(jsonPath("$.to").value("2026-07-31"))
                .andExpect(jsonPath("$.orders").value(679))
                .andExpect(jsonPath("$.revenue").value(480209.5));
    }

    @Test
    void onTimeReturnsOneRowPerCarrier() throws Exception {
        mvc.perform(get("/api/deliveries/on-time"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[1].carrier").value("Kessler Logistics"))
                .andExpect(jsonPath("$[1].delivered").value(265))
                .andExpect(jsonPath("$[1].onTime").value(238))
                .andExpect(jsonPath("$[1].rate").value(0.8981));
    }

    @Test
    void lateReturnsOrderCarrierDatesAndDaysLateAndRespectsTheLimit() throws Exception {
        mvc.perform(get("/api/deliveries/late").param("from", "2026-09-14").param("to", "2026-09-21").param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].orderRef").isString())
                .andExpect(jsonPath("$[0].carrier").isString())
                .andExpect(jsonPath("$[0].promisedDate").isString())
                .andExpect(jsonPath("$[0].deliveredDate").isString())
                .andExpect(jsonPath("$[0].daysLate").isNumber());
    }

    /** Formerly documented that from-after-to silently returned 200 []; TODO-232 makes it a 400. */
    @Test
    void lateWithFromAfterToIsRejectedWith400() throws Exception {
        mvc.perform(get("/api/deliveries/late").param("from", "2026-09-21").param("to", "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value("from must be on or before to"));
    }

    @Test
    void ticketsByCategoryReturnsOpenAndTotalPerCategory() throws Exception {
        mvc.perform(get("/api/tickets/by-category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].category").value("Delivery delay"))
                .andExpect(jsonPath("$[0].open").value(41))
                .andExpect(jsonPath("$[0].total").value(90));
    }

    @Test
    void vendorsIncludeDaysUntilContractEndAndTheNoticeWindowFlag() throws Exception {
        mvc.perform(get("/api/vendors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("$[0].name").value("Volta Parts GmbH"))
                .andExpect(jsonPath("$[0].contractEnd").value("2026-10-15"))
                .andExpect(jsonPath("$[0].noticeDays").value(30))
                .andExpect(jsonPath("$[0].daysUntilContractEnd").value(24))
                .andExpect(jsonPath("$[0].inNoticeWindow").value(true))
                .andExpect(jsonPath("$[7].inNoticeWindow").value(false));
    }

    /**
     * Formerly documented that a malformed date surfaced as a 500. TODO-232 turns it into a
     * 400 with an errors list. Kept as a real HTTP call so the rendered response (not just the
     * MockMvc dispatch) is checked.
     */
    @Test
    void malformedFromProducesA400WithAnErrorsListOverRealHttp() {
        ResponseEntity<String> response = http.getForEntity("/api/kpis?from=next-tuesday", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();
        assertThat(response.getBody()).isEqualTo("{\"errors\":[\"from must be an ISO date (YYYY-MM-DD)\"]}");
    }

    // ---- TODO-232: query parameter validation ----

    // AC-1
    @Test
    void malformedFromIsRejected() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "next-tuesday"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value("from must be an ISO date (YYYY-MM-DD)"));
    }

    // AC-1
    @Test
    void malformedToIsRejected() throws Exception {
        mvc.perform(get("/api/kpis").param("to", "2026/09/21"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value("to must be an ISO date (YYYY-MM-DD)"));
    }

    // AC-1 edge case: well-formed shape but not a real calendar date
    @Test
    void impossibleCalendarDateIsRejected() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "2026-02-30").param("to", "2026-03-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("from must be an ISO date (YYYY-MM-DD)"));
    }

    // AC-1 edge case: a malformed date skips the order and span checks
    @Test
    void malformedDateSkipsOrderAndSpanChecks() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "bad").param("to", "2020-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value("from must be an ISO date (YYYY-MM-DD)"));
    }

    // AC-2
    @Test
    void fromAfterToIsRejected() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "2026-09-02").param("to", "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value("from must be on or before to"));
    }

    // AC-2 edge case: a single-day range (from == to) is allowed
    @Test
    void singleDayRangeIsAccepted() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "2026-09-21").param("to", "2026-09-21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-09-21"))
                .andExpect(jsonPath("$.to").value("2026-09-21"));
    }

    // AC-2
    @Test
    void rangeOf367DaysIsRejected() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "2025-09-19").param("to", "2026-09-21"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value("range must span at most 366 days"));
    }

    // AC-2 boundary
    @Test
    void rangeOf366DaysIsAccepted() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "2025-09-20").param("to", "2026-09-21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2025-09-20"))
                .andExpect(jsonPath("$.to").value("2026-09-21"));
    }

    // AC-2: defaults still apply when only one bound is supplied
    @Test
    void missingToStillDefaultsToToday() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "2026-09-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-09-01"))
                .andExpect(jsonPath("$.to").value("2026-09-21"));
    }

    // AC-2 edge case: a supplied from after the default to (today) is rejected
    @Test
    void fromAfterTheDefaultToIsRejected() throws Exception {
        mvc.perform(get("/api/kpis").param("from", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("from must be on or before to"));
    }

    // AC-3
    @ParameterizedTest(name = "limit={0} is rejected")
    @ValueSource(strings = {"0", "501", "-1", "abc", "99999999999", "2.5"})
    void outOfRangeOrNonIntegerLimitIsRejected(String limit) throws Exception {
        mvc.perform(get("/api/deliveries/late").param("limit", limit))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0]").value("limit must be an integer between 1 and 500"));
    }

    // AC-3 boundaries
    @ParameterizedTest(name = "limit={0} is accepted")
    @ValueSource(strings = {"1", "500"})
    void limitAtTheBoundsIsAccepted(String limit) throws Exception {
        mvc.perform(get("/api/deliveries/late").param("from", "2026-09-14").param("to", "2026-09-21").param("limit", limit))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // AC-3: default stays 20
    @Test
    void limitDefaultsTo20() throws Exception {
        mvc.perform(get("/api/deliveries/late").param("from", "2025-09-21").param("to", "2026-09-21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(20)));
    }

    // AC-4
    @Test
    void severalProblemsProduceSeveralErrors() throws Exception {
        mvc.perform(get("/api/deliveries/late").param("from", "bad").param("to", "bad").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors", hasSize(3)))
                .andExpect(jsonPath("$.errors", containsInAnyOrder(
                        "from must be an ISO date (YYYY-MM-DD)",
                        "to must be an ISO date (YYYY-MM-DD)",
                        "limit must be an integer between 1 and 500")));
    }

    // AC-4: order error and limit error are reported together
    @Test
    void orderAndLimitErrorsAreReportedTogether() throws Exception {
        mvc.perform(get("/api/deliveries/late").param("from", "2026-09-21").param("to", "2026-09-01").param("limit", "501"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", containsInAnyOrder(
                        "from must be on or before to",
                        "limit must be an integer between 1 and 500")));
    }

    // AC-4: a valid request still works
    @Test
    void validRequestWithAllParametersStillWorks() throws Exception {
        mvc.perform(get("/api/deliveries/late").param("from", "2026-09-14").param("to", "2026-09-21").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // AC-4: the date rules apply to every endpoint that takes from/to
    @ParameterizedTest(name = "{0} validates from/to")
    @ValueSource(strings = {"/api/kpis", "/api/deliveries/on-time", "/api/deliveries/late", "/api/tickets/by-category"})
    void everyRangeEndpointRejectsMalformedDates(String endpoint) throws Exception {
        mvc.perform(get(endpoint).param("from", "next-tuesday"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("from must be an ISO date (YYYY-MM-DD)"));
    }

    @ParameterizedTest(name = "{0} validates from <= to")
    @ValueSource(strings = {"/api/kpis", "/api/deliveries/on-time", "/api/deliveries/late", "/api/tickets/by-category"})
    void everyRangeEndpointRejectsFromAfterTo(String endpoint) throws Exception {
        mvc.perform(get(endpoint).param("from", "2026-09-21").param("to", "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("from must be on or before to"));
    }

    @ParameterizedTest(name = "{0} validates the 366-day span")
    @ValueSource(strings = {"/api/kpis", "/api/deliveries/on-time", "/api/deliveries/late", "/api/tickets/by-category"})
    void everyRangeEndpointRejectsA367DayRange(String endpoint) throws Exception {
        mvc.perform(get(endpoint).param("from", "2025-09-19").param("to", "2026-09-21"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("range must span at most 366 days"));
    }

    @ParameterizedTest(name = "{0} accepts a valid range")
    @ValueSource(strings = {"/api/kpis", "/api/deliveries/on-time", "/api/deliveries/late", "/api/tickets/by-category"})
    void everyRangeEndpointAcceptsAValidRange(String endpoint) throws Exception {
        mvc.perform(get(endpoint).param("from", "2026-07-01").param("to", "2026-07-31"))
                .andExpect(status().isOk());
    }
}
