import java.util.ArrayList;
import java.util.List;

public class Ognl {
    public static List<Object> objToList(Object obj) {
        List<Object> list = new ArrayList<>();
        list.add(obj);
        list.add(obj);
        return list;
    }
}
