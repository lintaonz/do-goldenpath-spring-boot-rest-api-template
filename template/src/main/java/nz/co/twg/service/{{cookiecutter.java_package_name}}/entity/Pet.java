package nz.co.twg.service.{{cookiecutter.java_package_name}}.entity;

import com.google.common.base.Preconditions;
import javax.persistence.Entity;
import javax.persistence.Id;

/** Pet entity. */
@Entity
public class Pet {

    @Id private Long id;

    private String name;

    private String tag;

    /** Default constructor. Used by ORM. */
    @SuppressWarnings("NullAway.Init")
    public Pet() {}

    /** Constructor for convenience. */
    public Pet(Long id, String name, String tag) {
        Preconditions.checkArgument(id != null, "id cannot be null");
        Preconditions.checkArgument(name != null, "name cannot be null");
        Preconditions.checkArgument(tag != null, "tag cannot be null");

        this.id = id;
        this.name = name;
        this.tag = tag;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
