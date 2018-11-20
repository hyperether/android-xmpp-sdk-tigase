package org.tigase.messenger.phone.pro.utils;

import org.apache.commons.lang3.StringEscapeUtils;
import org.tigase.messenger.Constants;

public class Crypto {

    public static String encodeBody(String text) {
        if (text != null && Constants.ENCODE_BODY) {
//            try {
//                return URLEncoder.encode(text, "utf-8");
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//                return text;
//            }
            return convert(text);
        } else {
            return text;
        }
    }

    public static String decodeBody(String text) {
        if (text != null && Constants.ENCODE_BODY) {
//            try {
//                return URLDecoder.decode(text, "utf-8");
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//                return text;
//            }
            return convertToASCII(text);
        } else {
            return text;
        }
    }

    public static String convert(String str) {
        if (str != null) {
            StringBuffer ostr = new StringBuffer();
            for (int i = 0, length = str.length(); i < length; i++) {
                char ch = str.charAt(i);
                if (((ch >= 0x0020) &&
                        (ch <= 0x007e)) ||
                        ch == 0x000A)    // Does the char need to be converted to unicode?
                {
                    ostr.append(ch);                    // No.
                } else                                    // Yes.
                {
                    ostr.append("\\u");                // standard unicode format.
                    String hex = Integer
                            .toHexString(str.charAt(i) & 0xFFFF);    // Get hex value of the char.
                    for (int j = 0, len = 4 - hex.length();
                         j < len; j++)    // Prepend zeros because unicode requires 4 digits
                        ostr.append("0");
                    ostr.append(hex.toLowerCase());        // standard unicode format.
                }
            }
            return (new String(ostr));        //Return the stringbuffer cast as a string.
        }
        return null;
    }

    public static String convertToASCII(String escaped) {
        if (escaped != null) {
            if (escaped.indexOf("\\u") == -1)
                return escaped;

            escaped = StringEscapeUtils.unescapeJava(escaped);
            String processed = "";

            int position = escaped.indexOf("\\u");
            while (position != -1) {
                if (position != 0)
                    processed += escaped.substring(0, position);
                String token = escaped.substring(position + 2, position + 6);
                escaped = escaped.substring(position + 6);
                processed += (char) Integer.parseInt(token, 16);
                position = escaped.indexOf("\\u");
            }
            processed += escaped;

            return processed;
        }
        return "";
    }
}
