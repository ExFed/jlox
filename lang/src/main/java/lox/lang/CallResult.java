package lox.lang;

import lombok.Value;

@Value
class CallResult {
    boolean returning;
    Object value;
}
