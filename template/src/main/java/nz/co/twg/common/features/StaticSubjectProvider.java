package nz.co.twg.common.features;

/**
* Provider that returns a constant subject value Useful for services that does not rely on a
* security context or in testing.
*/
public final class StaticSubjectProvider implements SubjectProvider {

    private final String user;

    /**
    * Default constructor.
    *
    * @param user the static subject name
    */
    public StaticSubjectProvider(String user) {
        if (user == null) {
            throw new IllegalArgumentException("user cannot be null");
        }
        this.user = user;
    }

    @Override
    public String get() {
        return this.user;
    }
}
