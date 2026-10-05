package io.github.batslang.googleauthorize;

import static org.junit.Assert.assertEquals;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import org.junit.Test;

/** A failure's code is the status Play services names, or the exception's class. */
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
    public void anyOtherExceptionIsNamedByItsClass() {
        Failure failure = PlayAuthorizationService.failureOf(new IllegalArgumentException("no scopes"));
        assertEquals("IllegalArgumentException", failure.code);
        assertEquals("no scopes", failure.message);
    }
}
