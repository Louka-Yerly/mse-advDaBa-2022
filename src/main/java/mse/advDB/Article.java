package mse.advDB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Article {
    private String _id;
    private String title;
    private List<Author> authors;
    private List<String> references;

    private static final int TITLE_MAX_LENGTH = 20;

    private Map<String, Object> objectMap;

    public String get_id(){
        return _id;
    }

    public String getTitle(){
        return title;
    }

    public List<Author> getAuthors(){
        return authors;
    }

    public List<String> getReferences(){
        return references;
    }

    public void createObjectMap() {
        objectMap = new HashMap<>();
        objectMap.put("_id", _id);

        if(title == null) {
            title = "";
        }

        objectMap.put("title", title.substring(0, Integer.min(title.length(),TITLE_MAX_LENGTH)));

        if(authors != null) {
            List<String> authorsMapList = new ArrayList<>(authors.size());
            for(Author author: authors) {
                authorsMapList.add(author.get_id());
            }
            objectMap.put("authors", authorsMapList);
        } else{
            List<String> al = new ArrayList<>(1);
            al.add("");
            objectMap.put("authors", al);
        }

        if(references != null) {
            List<String> emptyString = new ArrayList<>();
            emptyString.add("");
            references.removeAll(emptyString);
            List<Map<String, Object>> refs = new ArrayList<>(references.size());
            objectMap.put("references", references);
        } else {
            List<String> refs = new ArrayList<>(1);
            refs.add("");
            objectMap.put("references", refs);
        }
    }

    public Map<String, Object> objectMap() {
        return objectMap;
    }

    @Override
    public String toString(){
        return "Book{" +
                "_id='" + _id + '\'' +
                ", title='" + title + '\'' +
                ", authors=" + authors +
                ", references=" + references +
                '}';
    }
}
