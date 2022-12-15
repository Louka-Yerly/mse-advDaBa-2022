package mse.advDB;

import java.util.HashMap;
import java.util.Map;

public class Author {
    private String _id;
    private String name;


    public String get_id(){
        return _id;
    }

    public String getName(){
        return name;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> res = new HashMap<>();
        if(_id == null) _id = "";
        if(name == null) name = "";
        res.put("_id", _id);
        res.put("name", name);
        return res;
    }

    @Override
    public String toString(){
        return "Author{" +
                "_id='" + _id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
