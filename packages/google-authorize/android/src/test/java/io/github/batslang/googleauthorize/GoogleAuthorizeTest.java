package io.github.batslang.googleauthorize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/** Each branch of GoogleAuthorize, over a stand-in for Play services' AuthorizationClient. */
public class GoogleAuthorizeTest {

    private static final List<String> SCOPES = List.of("https://www.googleapis.com/auth/drive.appdata");

    /** Records each call and keeps its reply, for the test to answer. */
    private static final class StandInService implements AuthorizationService<String, String> {

        final List<List<String>> authorized = new ArrayList<>();
        final List<Reply<Authorizing<String>>> authorizeReplies = new ArrayList<>();
        final List<String> consentsReturned = new ArrayList<>();
        final List<Reply<Authorization>> consentReplies = new ArrayList<>();
        final List<String> tokensCleared = new ArrayList<>();
        final List<Reply<Void>> clearReplies = new ArrayList<>();
        final List<String> accountsRevoked = new ArrayList<>();
        final List<List<String>> scopesRevoked = new ArrayList<>();
        final List<Reply<Void>> revokeReplies = new ArrayList<>();

        @Override
        public void authorize(List<String> scopes, Reply<Authorizing<String>> reply) {
            authorized.add(scopes);
            authorizeReplies.add(reply);
        }

        @Override
        public void authorizationFromConsent(String returned, Reply<Authorization> reply) {
            consentsReturned.add(returned);
            consentReplies.add(reply);
        }

        @Override
        public void clearToken(String accessToken, Reply<Void> reply) {
            tokensCleared.add(accessToken);
            clearReplies.add(reply);
        }

        @Override
        public void revokeAccess(String account, List<String> scopes, Reply<Void> reply) {
            accountsRevoked.add(account);
            scopesRevoked.add(scopes);
            revokeReplies.add(reply);
        }
    }

    /** What a call answered: exactly one of its kinds, once. */
    private static final class RecordedAnswer implements Answer {

        final List<String> kinds = new ArrayList<>();
        Authorization authorization;
        Failure failure;

        @Override
        public void authorized(Authorization given) {
            kinds.add("authorized");
            authorization = given;
        }

        @Override
        public void notAuthorized() {
            kinds.add("notAuthorized");
        }

        @Override
        public void done() {
            kinds.add("done");
        }

        @Override
        public void failed(Failure given) {
            kinds.add("failed");
            failure = given;
        }

        String only() {
            assertEquals("answered exactly once", 1, kinds.size());
            return kinds.get(0);
        }
    }

    private StandInService service;
    private List<String> consentsShown;
    private GoogleAuthorize<String, String> authorize;

    private static final Authorization GRANTED = new Authorization("token-1", SCOPES, "reader@example.com");

    @Before
    public void setUp() {
        service = new StandInService();
        consentsShown = new ArrayList<>();
        authorize = new GoogleAuthorize<>(service, consentsShown::add);
    }

    @Test
    public void authorizationForScopesGivesTheTokenWhenGranted() {
        RecordedAnswer answer = new RecordedAnswer();
        authorize.authorizationForScopes(SCOPES, answer);
        assertEquals(List.of(SCOPES), service.authorized);
        service.authorizeReplies.get(0).succeeded(new Authorizing.Granted<>(GRANTED));
        assertEquals("authorized", answer.only());
        assertSame(GRANTED, answer.authorization);
        assertTrue(consentsShown.isEmpty());
    }

    @Test
    public void authorizationForScopesGivesNoneAndShowsNothingWhenConsentIsNeeded() {
        RecordedAnswer answer = new RecordedAnswer();
        authorize.authorizationForScopes(SCOPES, answer);
        service.authorizeReplies.get(0).succeeded(new Authorizing.ConsentNeeded<>("consent"));
        assertEquals("notAuthorized", answer.only());
        assertTrue(consentsShown.isEmpty());
    }

    @Test
    public void authorizationForScopesFailsAsThePlatformDoes() {
        RecordedAnswer answer = new RecordedAnswer();
        authorize.authorizationForScopes(SCOPES, answer);
        service.authorizeReplies.get(0).failed(new Failure("NETWORK_ERROR", "offline"));
        assertEquals("failed", answer.only());
        assertEquals("NETWORK_ERROR", answer.failure.code);
        assertEquals("offline", answer.failure.message);
    }

    @Test
    public void authorizeScopesGivesTheTokenWithNoConsentWhenGranted() {
        RecordedAnswer answer = new RecordedAnswer();
        authorize.authorizeScopes(SCOPES, answer);
        service.authorizeReplies.get(0).succeeded(new Authorizing.Granted<>(GRANTED));
        assertEquals("authorized", answer.only());
        assertSame(GRANTED, answer.authorization);
        assertTrue(consentsShown.isEmpty());
    }

    @Test
    public void authorizeScopesShowsTheConsentThenGivesItsToken() {
        RecordedAnswer answer = new RecordedAnswer();
        authorize.authorizeScopes(SCOPES, answer);
        service.authorizeReplies.get(0).succeeded(new Authorizing.ConsentNeeded<>("consent"));
        assertEquals(List.of("consent"), consentsShown);
        assertTrue("no answer while the consent shows", answer.kinds.isEmpty());

        authorize.consentEnded(true, "returned");
        assertEquals(List.of("returned"), service.consentsReturned);
        service.consentReplies.get(0).succeeded(GRANTED);
        assertEquals("authorized", answer.only());
        assertSame(GRANTED, answer.authorization);
    }

    @Test
    public void authorizeScopesIsCanceledWhenTheReaderBacksOut() {
        RecordedAnswer answer = new RecordedAnswer();
        authorize.authorizeScopes(SCOPES, answer);
        service.authorizeReplies.get(0).succeeded(new Authorizing.ConsentNeeded<>("consent"));
        authorize.consentEnded(false, null);
        assertEquals("failed", answer.only());
        assertEquals(GoogleAuthorize.CANCELED, answer.failure.code);
        assertTrue("the result is not read", service.consentsReturned.isEmpty());
    }

    @Test
    public void authorizeScopesFailsWhenTheConsentsResultCannotBeRead() {
        RecordedAnswer answer = new RecordedAnswer();
        authorize.authorizeScopes(SCOPES, answer);
        service.authorizeReplies.get(0).succeeded(new Authorizing.ConsentNeeded<>("consent"));
        authorize.consentEnded(true, "returned");
        service.consentReplies.get(0).failed(new Failure("CANCELED", "16: "));
        assertEquals("failed", answer.only());
        assertEquals("CANCELED", answer.failure.code);
    }

    @Test
    public void authorizeScopesFailsAsThePlatformDoes() {
        RecordedAnswer answer = new RecordedAnswer();
        authorize.authorizeScopes(SCOPES, answer);
        service.authorizeReplies.get(0).failed(new Failure("DEVELOPER_ERROR", "no client for this app"));
        assertEquals("failed", answer.only());
        assertEquals("DEVELOPER_ERROR", answer.failure.code);
        assertTrue(consentsShown.isEmpty());
    }

    @Test
    public void aSecondConsentIsRefusedWhileOneShowsAndTheFirstStillEnds() {
        RecordedAnswer first = new RecordedAnswer();
        RecordedAnswer second = new RecordedAnswer();
        authorize.authorizeScopes(SCOPES, first);
        authorize.authorizeScopes(SCOPES, second);
        service.authorizeReplies.get(0).succeeded(new Authorizing.ConsentNeeded<>("first consent"));
        service.authorizeReplies.get(1).succeeded(new Authorizing.ConsentNeeded<>("second consent"));
        assertEquals(List.of("first consent"), consentsShown);
        assertEquals("failed", second.only());
        assertEquals(GoogleAuthorize.CONSENT_SHOWING, second.failure.code);

        authorize.consentEnded(true, "returned");
        service.consentReplies.get(0).succeeded(GRANTED);
        assertEquals("authorized", first.only());
    }

    @Test
    public void aConsentCanShowAgainOnceTheLastEnded() {
        RecordedAnswer first = new RecordedAnswer();
        authorize.authorizeScopes(SCOPES, first);
        service.authorizeReplies.get(0).succeeded(new Authorizing.ConsentNeeded<>("first consent"));
        authorize.consentEnded(false, null);

        RecordedAnswer second = new RecordedAnswer();
        authorize.authorizeScopes(SCOPES, second);
        service.authorizeReplies.get(1).succeeded(new Authorizing.ConsentNeeded<>("second consent"));
        assertEquals(List.of("first consent", "second consent"), consentsShown);
        assertTrue(second.kinds.isEmpty());
    }

    @Test
    public void aConsentResultWithNoCallWaitingIsDropped() {
        authorize.consentEnded(true, "returned");
        assertTrue(service.consentsReturned.isEmpty());
    }

    @Test
    public void clearAuthorizationTokenClearsThatToken() {
        RecordedAnswer answer = new RecordedAnswer();
        authorize.clearAuthorizationToken("token-1", answer);
        assertEquals(List.of("token-1"), service.tokensCleared);
        service.clearReplies.get(0).succeeded(null);
        assertEquals("done", answer.only());
    }

    @Test
    public void clearAuthorizationTokenFailsAsThePlatformDoes() {
        RecordedAnswer answer = new RecordedAnswer();
        authorize.clearAuthorizationToken("token-1", answer);
        service.clearReplies.get(0).failed(new Failure("INTERNAL_ERROR", "failed"));
        assertEquals("failed", answer.only());
        assertEquals("INTERNAL_ERROR", answer.failure.code);
    }

    @Test
    public void revokeAccessRevokesThatAccountsScopes() {
        RecordedAnswer answer = new RecordedAnswer();
        authorize.revokeAccess("reader@example.com", SCOPES, answer);
        assertEquals(List.of("reader@example.com"), service.accountsRevoked);
        assertEquals(List.of(SCOPES), service.scopesRevoked);
        service.revokeReplies.get(0).succeeded(null);
        assertEquals("done", answer.only());
    }

    @Test
    public void revokeAccessFailsAsThePlatformDoes() {
        RecordedAnswer answer = new RecordedAnswer();
        authorize.revokeAccess("reader@example.com", SCOPES, answer);
        service.revokeReplies.get(0).failed(new Failure("NETWORK_ERROR", "offline"));
        assertEquals("failed", answer.only());
        assertEquals("NETWORK_ERROR", answer.failure.code);
    }

    @Test
    public void aGrantWithNoAccountKeepsItNull() {
        RecordedAnswer answer = new RecordedAnswer();
        authorize.authorizationForScopes(SCOPES, answer);
        service.authorizeReplies
            .get(0)
            .succeeded(new Authorizing.Granted<>(new Authorization("token-2", SCOPES, null)));
        assertEquals("authorized", answer.only());
        assertNull(answer.authorization.account);
    }
}
