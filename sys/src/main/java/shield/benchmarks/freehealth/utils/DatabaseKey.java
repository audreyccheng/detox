package shield.benchmarks.freehealth.utils;

public abstract class DatabaseKey {

    /**
     * Returns a Integer version of the key (some form of hashing is used)
     */
    public abstract Integer value();

    public String str() {
        return value().toString();
    }

};

class NameKey extends DatabaseKey {
    private String name;

    NameKey(String name) {
        this.name = name;
    }

    @Override
    public Integer value() {
        return name.hashCode();
    }

    @Override
    public String str() { return name; }
};
