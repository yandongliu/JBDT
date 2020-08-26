package JBDT.data;

public class Pair<U,V> {
    public U first;
    public V second;

    public Pair() {

    }
    public Pair(U u, V v) {
        this.first = u;
        this.second = v;
    }
}
