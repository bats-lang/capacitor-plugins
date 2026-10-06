package io.github.batslang.googleauthorize;

import static org.junit.Assert.assertEquals;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import org.junit.Test;

/** A failure's code is the status Play services names; anything else is UNEXPECTED. */
public class PlayAuthorizationServiceTest {

    @Test
    public void anApiExceptionIsNamedByItsStatus() {
        Failure failure = PlayAuthorizationService.failureOf(
            new ApiException(new Status(CommonStatusCodes.NETWORK_ERROR))
        );
        assertEquals("NETWORK_ERROR", failure.code);
    }

    @Test
    public void aCanceledStatusIsCanceled() {
        Failure failure = PlayAuthorizationService.failureOf(new ApiException(new Status(CommonStatusCodes.CANCELED)));
        assertEquals("CANCELED", failure.code);
    }

    @Test
    public void aDeveloperErrorIsNamedWithItsMessage() {
        Failure failure = PlayAuthorizationService.failureOf(
            new ApiException(new Status(CommonStatusCodes.DEVELOPER_ERROR, "no client for this app"))
        );
        assertEquals("DEVELOPER_ERROR", failure.code);
        assertEquals("10: no client for this app", failure.message);
    }

    @Test
    public void aStatusCommonStatusCodesDoesNotNameIsUnexpected() {
        Failure failure = PlayAuthorizationService.failureOf(
            new ApiException(new Status(7000, "new in Play services"))
        );
        assertEquals(GoogleAuthorize.UNEXPECTED, failure.code);
        assertEquals("Status code 7000: 7000: new in Play services", failure.message);
    }

    @Test
    public void anyOtherExceptionIsUnexpectedAndNamedInItsMessage() {
        Failure failure = PlayAuthorizationService.failureOf(new IllegalArgumentException("no scopes"));
        assertEquals(GoogleAuthorize.UNEXPECTED, failure.code);
        assertEquals("IllegalArgumentException: no scopes", failure.message);
    }
}
