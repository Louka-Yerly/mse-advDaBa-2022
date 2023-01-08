package mse.advDB;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Author {
    private String _id;
    private String name;


    public String get_id(){
        return _id == null? "" : _id;
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

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        Author author = (Author) o;
        return Objects.equals(_id, author._id);
    }

    @Override
    public int hashCode(){
        return Objects.hash(_id);
    }
}
