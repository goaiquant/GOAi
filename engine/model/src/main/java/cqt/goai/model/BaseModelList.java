package cqt.goai.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * 处理list情况
 *
 * 封装基本list，额外添加 to 和 of 方法
 * Iterator Pattern 迭代器模式
 *
 * @author GOAi
 * @param <E> list的对象
 */
@Getter
@EqualsAndHashCode
public abstract class BaseModelList<E extends To> implements To, Iterable<E>, Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 底层数据
     */
    private List<E> list;

    /**
     * 构造器传入list对象，可选择ArrayList或LinkedList，看情况选择
     * @param list list
     */
    public BaseModelList(List<E> list) {
        this.list = list;
    }

    @Override
    public String to() {
        return Util.to(this.list);
    }


    public int size() {
        return this.list.size();
    }

    public boolean isEmpty() {
        return this.list.isEmpty();
    }

    public boolean contains(E e) {
        return this.list.contains(e);
    }

    @Override
    public Iterator<E> iterator() {
        return this.list.iterator();
    }

    public Object[] toArray() {
        return this.list.toArray();
    }

    public E[] toArray(E[] a) {
        return this.list.toArray(a);
    }

    public E get(int index) {
        return this.list.get(index);
    }

    public E remove(int index) {
        return this.list.remove(index);
    }

    public int indexOf(E e) {
        return this.list.indexOf(e);
    }

    public int lastIndexOf(E e) {
        return this.list.lastIndexOf(e);
    }

    public Stream<E> stream() {
        return this.list.stream();
    }

    public Stream<E> parallelStream() {
        return this.list.parallelStream();
    }

    public E first() {
        return this.get(0);
    }

    public E last() {
        return this.get(this.size() - 1);
    }

    public E getFirst() {
        return this.first();
    }

    public E getLast() {
        return this.last();
    }

    public void add(E e) {
        this.list.add(e);
    }

    public void set(List<E> eList) {
        this.list = eList;
    }

    @Override
    public String toString() {
        return list.toString();
    }
}
