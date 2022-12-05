package nz.co.twg.service.{{cookiecutter.java_package_name}}.componenttest.util;

import com.google.common.base.Preconditions;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
* This object describes a piece of data that is encrypted by the TWG Encryption Service. It is use
* to setup a simulated API in WireMock for the application to use in testing.
*
* @see WireMockHelper#stubEncryption(List)
*/
public class EncryptionMockData {

    private final String uniqueId;

    /** This is the object to encrypt. */
    private final Object source;

    /** This is the result of the encryption; Base64 in the case of mock encryption. */
    private final String targetBase64;

    public EncryptionMockData(String uniqueId, Object source, String targetBase64) {
        Preconditions.checkArgument(StringUtils.isNotBlank(uniqueId), "bad uniqueId");
        this.uniqueId = uniqueId;
        this.source = source;
        this.targetBase64 = targetBase64;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public Object getSource() {
        return source;
    }

    public String getTarget() {
        return targetBase64;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SIMPLE_STYLE)
                .append("uniqueId", uniqueId)
                .toString();
    }
}
