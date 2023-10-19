package com.itextpdf.kernel.crypto.securityhandler;

import com.itextpdf.kernel.pdf.PdfArray;
import com.itextpdf.kernel.pdf.PdfBoolean;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfNumber;
import com.itextpdf.kernel.security.IExternalDecryptionProcess;
import java.security.Key;
import java.security.cert.Certificate;

public class PubSecHandlerUsingStandard128 extends PubSecHandlerUsingStandard40 {
    private static final long serialVersionUID = 4243832116977499452L;

    public PubSecHandlerUsingStandard128(PdfDictionary encryptionDictionary, Certificate[] certs, int[] permissions, boolean encryptMetadata, boolean embeddedFilesOnly) {
        super(encryptionDictionary, certs, permissions, encryptMetadata, embeddedFilesOnly);
    }

    public PubSecHandlerUsingStandard128(PdfDictionary encryptionDictionary, Key certificateKey, Certificate certificate, String certificateKeyProvider, IExternalDecryptionProcess externalDecryptionProcess, boolean encryptMetadata) {
        super(encryptionDictionary, certificateKey, certificate, certificateKeyProvider, externalDecryptionProcess, encryptMetadata);
    }

    /* access modifiers changed from: protected */
    public void setPubSecSpecificHandlerDicEntries(PdfDictionary encryptionDictionary, boolean encryptMetadata, boolean embeddedFilesOnly) {
        encryptionDictionary.put(PdfName.Filter, PdfName.Adobe_PubSec);
        PdfArray recipients = createRecipientsArray();
        if (encryptMetadata) {
            encryptionDictionary.put(PdfName.f1376R, new PdfNumber(3));
            encryptionDictionary.put(PdfName.f1406V, new PdfNumber(2));
            encryptionDictionary.put(PdfName.SubFilter, PdfName.Adbe_pkcs7_s4);
            encryptionDictionary.put(PdfName.Recipients, recipients);
            return;
        }
        encryptionDictionary.put(PdfName.f1376R, new PdfNumber(4));
        encryptionDictionary.put(PdfName.f1406V, new PdfNumber(4));
        encryptionDictionary.put(PdfName.SubFilter, PdfName.Adbe_pkcs7_s5);
        PdfDictionary stdcf = new PdfDictionary();
        stdcf.put(PdfName.Recipients, recipients);
        stdcf.put(PdfName.EncryptMetadata, PdfBoolean.FALSE);
        stdcf.put(PdfName.CFM, PdfName.f1407V2);
        PdfDictionary cf = new PdfDictionary();
        cf.put(PdfName.DefaultCryptFilter, stdcf);
        encryptionDictionary.put(PdfName.f1304CF, cf);
        if (embeddedFilesOnly) {
            encryptionDictionary.put(PdfName.EFF, PdfName.DefaultCryptFilter);
            encryptionDictionary.put(PdfName.StrF, PdfName.Identity);
            encryptionDictionary.put(PdfName.StmF, PdfName.Identity);
            return;
        }
        encryptionDictionary.put(PdfName.StrF, PdfName.DefaultCryptFilter);
        encryptionDictionary.put(PdfName.StmF, PdfName.DefaultCryptFilter);
    }
}
