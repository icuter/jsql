package cn.icuter.jsql.security;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class InjectionWords {
    private static final String[] WORDS = new String[] {";", "--", "/*", "#", "%", "?", "@", "'", "\""};
    private static final InjectionWords INSTANCE = new InjectionWords();
    private TrieNode root;
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Lock readLock = lock.readLock();
    private Lock writeLock = lock.writeLock();

    private InjectionWords() {
        resetWords(WORDS);
    }
    static InjectionWords getInstance() {
        return INSTANCE;
    }
    void resetWords(String[] words) {
        writeLock.lock();
        try {
            root = new TrieNode((char) 0);
            buildTrie(words);
            buildFail();
        } finally {
            writeLock.unlock();
        }
    }
    void addWords(String[] words) {
        writeLock.lock();
        try {
            buildTrie(words);
            buildFail();
        } finally {
            writeLock.unlock();
        }
    }
    boolean detect(String src) {
        readLock.lock();
        try {
            TrieNode streamNode = root;
            for (int i = 0; i < src.length(); i++) {
                char c = src.charAt(i);
                TrieNode firstNode = streamNode.children.get(c);
                TrieNode parent = streamNode;
                while (parent.fail != null && firstNode == null) {
                    parent = parent.fail;
                    firstNode = parent.children.get(c);
                }
                streamNode = firstNode != null ? firstNode : root;
                while (firstNode != null && !firstNode.isWord) {
                    firstNode = firstNode.fail;
                }
                if (firstNode != null) {
                    return true;
                }
            }
            return false;
        } finally {
            readLock.unlock();
        }
    }
    private void buildTrie(String[] words) {
        for (String word : words) {
            if (word == null || word.length() <= 0) {
                return;
            }
            TrieNode current = root;
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                if (current.children.get(c) == null) {
                    current.children.put(c, new TrieNode(c));
                }
                current = current.children.get(c);
            }
            current.isWord = true;
        }
    }
    private void buildFail() {
        Queue<TrieNode> queue = new LinkedList<TrieNode>();
        queue.add(root);
        while (!queue.isEmpty()) {
            TrieNode parent = queue.poll();
            for (TrieNode child : parent.children.values()) {
                TrieNode fail = parent.fail;
                while (fail != null) {
                    TrieNode failChild = fail.children.get(child.val);
                    if (failChild != null) {
                        child.fail = failChild;
                        break;
                    }
                    fail = fail.fail;
                }
                if (child.fail == null) {
                    child.fail = root;
                }
                queue.add(child);
            }
        }
    }
    static class TrieNode {
        TrieNode(char val) {
            this.val = val;
        }
        char val;      // root val is (char) 0
        TrieNode fail; // root.fail is null
        Map<Character, TrieNode> children = new HashMap<Character, TrieNode>();
        boolean isWord;
    }
}
