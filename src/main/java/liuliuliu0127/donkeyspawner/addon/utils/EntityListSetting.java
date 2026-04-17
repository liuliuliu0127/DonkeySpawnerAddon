package liuliuliu0127.donkeyspawner.addon.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EntityListSetting {
    private List<String> list;
    private final Consumer<List<String>> onUpdate;

    public EntityListSetting(List<String> defaultList, Consumer<List<String>> onUpdate) {
        this.list = new ArrayList<>(defaultList);
        this.onUpdate = onUpdate;
    }

    public List<String> get() {
        return list;
    }

    public void set(Iterable<String> newList) {
        this.list.clear();
        newList.forEach(this.list::add);
        if (onUpdate != null) onUpdate.accept(this.list);
    }
}
