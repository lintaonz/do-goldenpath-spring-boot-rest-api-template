package nz.co.twg.features;

/**
* Provider that returns a constant subject value Useful for services that does not rely on a
* security context or in testing
*/
public final class StaticSubjectProvider implements SubjectProvider {

    private final String user;

    public StaticSubjectProvider(String user) {
        assert user != null; // use precondition?
        this.user = user;
    }

    @Override
    public String get() {
        return this.user;
    }
}
