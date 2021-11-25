package nz.co.twg.service.{{cookiecutter.java_package_name}}.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Pet {

    @Id private Long id;

    private String name;

    private String tag;

    public Pet() {}

    public Pet(Long id, String name, String tag) {
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
