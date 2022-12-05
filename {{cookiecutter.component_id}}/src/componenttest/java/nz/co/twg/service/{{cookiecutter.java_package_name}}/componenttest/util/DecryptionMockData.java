package nz.co.twg.service.{{cookiecutter.java_package_name}}.componenttest.util;

import com.google.common.base.Preconditions;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
* This object describes a piece of data that is decrypted by the TWG Encryption Service. It is use
* to setup a simulated API in WireMock * for the application to use in testing.
*
* @see WireMockHelper#stubDecryption(List)
*/
public class DecryptionMockData {

    private final String uniqueId;

    /** This is the data that will be decrypted. It is encoded as Base64. */
    private final String sourceBase64;

    /**
    * This member variable is the data that would be returned once the data is descrypted. It would
    * be returned in a response from the TWG Encryption Service.
    */
    private final Object target;

    public DecryptionMockData(String uniqueId, String sourceBase64, Object target) {
        Preconditions.checkArgument(StringUtils.isNotBlank(uniqueId), "bad uniqueId");
        this.uniqueId = uniqueId;
        this.sourceBase64 = sourceBase64;
        this.target = target;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public String getSource() {
        return sourceBase64;
    }

    public Object getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SIMPLE_STYLE)
                .append("uniqueId", uniqueId)
                .toString();
    }
}
