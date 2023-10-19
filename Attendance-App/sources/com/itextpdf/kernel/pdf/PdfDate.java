package com.itextpdf.kernel.pdf;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import org.bouncycastle.pqc.math.linearalgebra.Matrix;
import org.slf4j.Marker;

public class PdfDate extends PdfObjectWrapper<PdfString> {
    private static final int[] DATE_SPACE = {1, 4, 0, 2, 2, -1, 5, 2, 0, 11, 2, 0, 12, 2, 0, 13, 2, 0};
    private static final long serialVersionUID = -7424858548790000216L;

    public PdfDate(Calendar d) {
        super(new PdfString(generateStringByCalendar(d)));
    }

    public PdfDate() {
        this(new GregorianCalendar());
    }

    public String getW3CDate() {
        return getW3CDate(((PdfString) getPdfObject()).getValue());
    }

    public static String getW3CDate(String d) {
        if (d.startsWith("D:")) {
            d = d.substring(2);
        }
        StringBuilder sb = new StringBuilder();
        if (d.length() < 4) {
            return "0000";
        }
        sb.append(d.substring(0, 4));
        String d2 = d.substring(4);
        if (d2.length() < 2) {
            return sb.toString();
        }
        sb.append('-').append(d2.substring(0, 2));
        String d3 = d2.substring(2);
        if (d3.length() < 2) {
            return sb.toString();
        }
        sb.append('-').append(d3.substring(0, 2));
        String d4 = d3.substring(2);
        if (d4.length() < 2) {
            return sb.toString();
        }
        sb.append('T').append(d4.substring(0, 2));
        String d5 = d4.substring(2);
        if (d5.length() < 2) {
            sb.append(":00Z");
            return sb.toString();
        }
        sb.append(':').append(d5.substring(0, 2));
        String d6 = d5.substring(2);
        if (d6.length() < 2) {
            sb.append(Matrix.MATRIX_TYPE_ZERO);
            return sb.toString();
        }
        sb.append(':').append(d6.substring(0, 2));
        String d7 = d6.substring(2);
        if (d7.startsWith("-") || d7.startsWith(Marker.ANY_NON_NULL_MARKER)) {
            String sign = d7.substring(0, 1);
            String d8 = d7.substring(1);
            if (d8.length() >= 2) {
                String h = d8.substring(0, 2);
                String m = "00";
                if (d8.length() > 2) {
                    String d9 = d8.substring(3);
                    if (d9.length() >= 2) {
                        m = d9.substring(0, 2);
                    }
                }
                sb.append(sign).append(h).append(':').append(m);
                return sb.toString();
            }
        }
        sb.append(Matrix.MATRIX_TYPE_ZERO);
        return sb.toString();
    }

    public static Calendar decode(String s) {
        GregorianCalendar calendar;
        try {
            if (s.startsWith("D:")) {
                s = s.substring(2);
            }
            int slen = s.length();
            int idx = s.indexOf(90);
            if (idx >= 0) {
                slen = idx;
                calendar = new GregorianCalendar(new SimpleTimeZone(0, "ZPDF"));
            } else {
                int sign = 1;
                int idx2 = s.indexOf(43);
                if (idx2 < 0 && (idx2 = s.indexOf(45)) >= 0) {
                    sign = -1;
                }
                if (idx2 < 0) {
                    calendar = new GregorianCalendar();
                } else {
                    int offset = Integer.parseInt(s.substring(idx2 + 1, idx2 + 3)) * 60;
                    if (idx2 + 5 < s.length()) {
                        offset += Integer.parseInt(s.substring(idx2 + 4, idx2 + 6));
                    }
                    calendar = new GregorianCalendar(new SimpleTimeZone(offset * sign * 60000, "ZPDF"));
                    slen = idx2;
                }
            }
            calendar.clear();
            int idx3 = 0;
            int k = 0;
            while (true) {
                int[] iArr = DATE_SPACE;
                if (k >= iArr.length) {
                    break;
                } else if (idx3 >= slen) {
                    break;
                } else {
                    calendar.set(iArr[k], Integer.parseInt(s.substring(idx3, iArr[k + 1] + idx3)) + iArr[k + 2]);
                    idx3 += iArr[k + 1];
                    k += 3;
                }
            }
            return calendar;
        } catch (Exception e) {
            return null;
        }
    }

    /* access modifiers changed from: protected */
    public boolean isWrappedObjectMustBeIndirect() {
        return false;
    }

    private static String generateStringByCalendar(Calendar d) {
        StringBuilder date = new StringBuilder("D:");
        date.append(setLength(d.get(1), 4));
        date.append(setLength(d.get(2) + 1, 2));
        date.append(setLength(d.get(5), 2));
        date.append(setLength(d.get(11), 2));
        date.append(setLength(d.get(12), 2));
        date.append(setLength(d.get(13), 2));
        int timezone = (d.get(15) + d.get(16)) / 3600000;
        if (timezone == 0) {
            date.append(Matrix.MATRIX_TYPE_ZERO);
        } else if (timezone < 0) {
            date.append('-');
            timezone = -timezone;
        } else {
            date.append('+');
        }
        if (timezone != 0) {
            date.append(setLength(timezone, 2)).append('\'');
            date.append(setLength(Math.abs((d.get(15) + d.get(16)) / 60000) - (timezone * 60), 2)).append('\'');
        }
        return date.toString();
    }

    private static String setLength(int i, int length) {
        StringBuilder tmp = new StringBuilder();
        tmp.append(i);
        while (tmp.length() < length) {
            tmp.insert(0, "0");
        }
        tmp.setLength(length);
        return tmp.toString();
    }
}
