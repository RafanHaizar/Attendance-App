package com.itextpdf.kernel.pdf;

import com.itextpdf.p026io.LogMessageConstant;
import java.text.MessageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersionConforming {
    public static final String DEPRECATED_AES256_REVISION = "It seems that PDF 1.7 document encrypted with AES256 was updated to PDF 2.0 version and StampingProperties#preserveEncryption flag was set: encryption shall be updated via WriterProperties#setStandardEncryption method. Standard security handler was found with revision 5, which is deprecated and shall not be used in PDF 2.0 documents.";
    public static final String DEPRECATED_ENCRYPTION_ALGORITHMS = "Encryption algorithms STANDARD_ENCRYPTION_40, STANDARD_ENCRYPTION_128 and ENCRYPTION_AES_128 (see com.itextpdf.kernel.pdf.EncryptionConstants) are deprecated in PDF 2.0. It is highly recommended not to use it.";
    public static final String DEPRECATED_NEED_APPEARANCES_IN_ACROFORM = "NeedAppearances has been deprecated in PDF 2.0. Appearance streams are required in PDF 2.0.";
    public static final String DEPRECATED_XFA_FORMS = "XFA is deprecated in PDF 2.0. The XFA form will not be written to the document";
    private static final Logger logger = LoggerFactory.getLogger((Class<?>) VersionConforming.class);

    public static boolean validatePdfVersionForDictEntry(PdfDocument document, PdfVersion expectedVersion, PdfName entryKey, PdfName dictType) {
        if (document == null || document.getPdfVersion().compareTo(expectedVersion) >= 0) {
            return false;
        }
        logger.warn(MessageFormat.format(LogMessageConstant.VERSION_INCOMPATIBILITY_FOR_DICTIONARY_ENTRY, new Object[]{entryKey, dictType, expectedVersion, document.getPdfVersion()}));
        return true;
    }

    public static boolean validatePdfVersionForDeprecatedFeatureLogWarn(PdfDocument document, PdfVersion expectedVersion, String deprecatedFeatureLogMessage) {
        if (document.getPdfVersion().compareTo(expectedVersion) < 0) {
            return false;
        }
        logger.warn(deprecatedFeatureLogMessage);
        return true;
    }

    public static boolean validatePdfVersionForDeprecatedFeatureLogError(PdfDocument document, PdfVersion expectedVersion, String deprecatedFeatureLogMessage) {
        if (document.getPdfVersion().compareTo(expectedVersion) < 0) {
            return false;
        }
        logger.error(deprecatedFeatureLogMessage);
        return true;
    }
}
