package tap.communication;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

class HTTPRequestException extends IOException {
    private final int responseCode;

    public HTTPRequestException(int responseCode, String message) {
        super(message);
        this.responseCode = responseCode;
    }

    public int getResponseCode() {
        return responseCode;
    }
}