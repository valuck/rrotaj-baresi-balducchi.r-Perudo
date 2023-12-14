package org.perudo;

import Messaging.Message;
import Messaging.User;

import java.util.LinkedHashMap;

public class Main {
    public static void main(String[] args) {
        // Server testing
        new Thread(new Server(3000)).start();

        LinkedHashMap<String, Integer> data = new LinkedHashMap<>();
        data.put("Mat", 10);
        data.put("Sos", 20);

        User user = new User();
        user.setEncodingKey("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxGgZiGV/qwYYlpCOCLHsMULNdd4j96HKPZHg5gM7w9LcEcu0K+3g6eSMoLQ0YEFycc7n9zK9GE4TvT6v3VFvIwFKUfBBQ5OqmJjVD3mYx8M4JzGA/naUg7cIqEVJ33Wv/AkJNGDmnsyLfQbE3Xsr8mks/RT9Opqt0Re/zq5jmGJHvlao+5ownF/h3nVho6J0M5itvI32fg9Avajjs7xWR04k+YbZzSPb1GVQAgeEciJI1Sqky1d2a86byVFezUFI8rDuoyZ6SMXf/coGlX6z8jb5jct0avu7pgWrUHWgjT+Xv6KgEePHk+Z7Hi/0y6/4DdTlHanj/wO4tH1r07DImQIDAQAB");

        /*
        Private: MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDEaBmIZX+rBhiWkI4IsewxQs113iP3oco9keDmAzvD0twRy7Qr7eDp5IygtDRgQXJxzuf3Mr0YThO9Pq/dUW8jAUpR8EFDk6qYmNUPeZjHwzgnMYD+dpSDtwioRUnfda/8CQk0YOaezIt9BsTdeyvyaSz9FP06mq3RF7/OrmOYYke+Vqj7mjCcX+HedWGjonQzmK28jfZ+D0C9qOOzvFZHTiT5htnNI9vUZVACB4RyIkjVKqTLV3ZrzpvJUV7NQUjysO6jJnpIxd/9ygaVfrPyNvmNy3Rq+7umBatQdaCNP5e/oqAR48eT5nseL/TLr/gN1OUdqeP/A7i0fWvTsMiZAgMBAAECggEAA/SXWQwd2IwLodKg9Az5qJqDqHh6dg6YoLzn0AunuZ9tDHp///RI0VhBOpMbTLn2S+z13OBbqkB+SZNYd6cf2Bv65KOnfAZecYRlPBKZY7wegCMq1ohJ7U1Z6KIJy6zwCAeHKC2KAqlnZya9zCEVwKP8gpwYdBFXGuaOl6/vtD5AOBqDsfxp6/OcaaWNG/D24dQvNmiyqYvvyXySjLDa7+c5Vtt5a77MnFif6SxLsPMD+Zp5nQoOY9sLGLxXemU5U82jGK52fu/A2S87qYWQq+Z6iFo3+OSc1a9QuqX1qaTd9VyKNDzL34ZI79cbROV6gxv5oioDav5UUrt+j+1TZQKBgQDoc9yYShFll7Q5yQUCApo4no/yyCsFeCs1QqzRe5gbWIi/6isjzoaEHRByaGRo76SD8WxNnJRCwvoHArYH59e/zNwn/N+d1QRHXhGwt31zQe295ik2iWxm0kZh85s/tFTacs1qg37TUFxOFe6fFqRkaeo4DjaTJHFMkqfEGQyxBQKBgQDYTXeNuwvhYQdUZcjn90q80epWlLQK1yB8hEEQ4N4eD1XFe1p0UzxwNt+pvrinYFVXw2QHYle90OB/6NzWaKzdcqWTVVqytjef6NZhdJnX7VNhacAHh+T6tpDSBnl5Gfw5f3AOKEiEcOVkzQy9hkWZP4ZU28Uh2wm5jy1MNsJdhQKBgQDnadEG21YOacTGclVF9wlJrLFp5dTOkR+S2lnqAr0fHDintpxT/Vr1U2n4O+0aetixcjt72JHRFQKM0CogHLMxAuV5+ck0+8hStFewMjv17oCcOPJ2lMESa+wdv8Oi3AhNoVTW4Sn1fV8nbbYtMwFd5gOC4sN6ISGaNbZHBxkXcQKBgBwH5f9jBwYdGeMREbkl7fSMS48O1Eo3WMlqJjh2u7IvkJD4DF/YzZNJOTD5O1rNfCHUe2kNkItTv1Q0LDJI/GXyTozKWeeg/5JkjSux/aHi6pRMLQuMy9rFv/zHxAQynuIgprfe9o7bRo8Kl6UT+n8CoXjrxh8+EIOQj3E6A3Q1AoGBAMUOHBDKESIHnYJNe/daBT8mnsUiGy65FzOfa3mDD5nLAH4tqnaPbs7XwjA8Ihaw+f90VOMfdsgKSET4rmfTsjtjDC+iytBXvir9g1Zi/uY227Shr8+eLdE8gIadiFZaiVU5vGDeQ4yegVlyU96SBnldKnLkDP3mxQo8WQznCLvV
        Public: MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxGgZiGV/qwYYlpCOCLHsMULNdd4j96HKPZHg5gM7w9LcEcu0K+3g6eSMoLQ0YEFycc7n9zK9GE4TvT6v3VFvIwFKUfBBQ5OqmJjVD3mYx8M4JzGA/naUg7cIqEVJ33Wv/AkJNGDmnsyLfQbE3Xsr8mks/RT9Opqt0Re/zq5jmGJHvlao+5ownF/h3nVho6J0M5itvI32fg9Avajjs7xWR04k+YbZzSPb1GVQAgeEciJI1Sqky1d2a86byVFezUFI8rDuoyZ6SMXf/coGlX6z8jb5jct0avu7pgWrUHWgjT+Xv6KgEePHk+Z7Hi/0y6/4DdTlHanj/wO4tH1r07DImQIDAQAB
         */

        Message<LinkedHashMap<String, Integer>> msg = new Message<>(user, "Alura", data, true);
        System.out.println(msg.toJson());
    }
}