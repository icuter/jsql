package cn.icuter.jsql.security;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class InjectionWords {
    public static final String[] DEFAULT_WORDS = new String[] {";", "--", "/*", "#", "%", "?", "@", "'", "\""};
    private static final InjectionWords INSTANCE = new InjectionWords();
    private transient volatile TrieNode root; // as a snapshot
    List<String> wordList = new LinkedList<>();
    private Lock lock = new ReentrantLock();

    private InjectionWords() {
        resetWords(DEFAULT_WORDS);
    }

    static InjectionWords getInstance() {
        return INSTANCE;
    }

    void resetWords(String[] words) {
        lock.lock();
        try {
            wordList.clear();
            wordList.addAll(Arrays.asList(words));
            resetRoot();
        } finally {
            lock.unlock();
        }
    }

    void addWords(String[] words) {
        lock.lock();
        try {
            wordList.addAll(Arrays.asList(words));
            resetRoot();
        } finally {
            lock.unlock();
        }
    }

    private void resetRoot() {
        TrieNode tempRoot = new TrieNode((char) 0);
        buildTrie(tempRoot, wordList);
        buildFail(tempRoot);
        root = tempRoot;
    }

    boolean detect(String src) {
        TrieNode snapshotRoot = getRoot();
        TrieNode streamNode = snapshotRoot;
        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            TrieNode firstNode = streamNode.children.get(c);
            TrieNode parent = streamNode;
            while (parent.fail != null && firstNode == null) {
                parent = parent.fail;
                firstNode = parent.children.get(c);
            }
            streamNode = firstNode != null ? firstNode : snapshotRoot;
            while (firstNode != null && !firstNode.isWord) {
                firstNode = firstNode.fail;
            }
            if (firstNode != null) {
                return true;
            }
        }
        return false;
    }

    TrieNode getRoot() {
        return root;
    }

    private void buildTrie(TrieNode r, List<String> words) {
        for (String word : words) {
            if (word == null || word.length() <= 0) {
                return;
            }
            TrieNode current = r;
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

    private void buildFail(TrieNode r) {
        Objects.requireNonNull(r, "Root Parameter must not be null !");

        Queue<TrieNode> queue = new LinkedList<>();
        queue.add(r);
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
                    child.fail = r;
                }
                queue.add(child);
            }
        }
    }
    static class TrieNode {
        TrieNode(char val) {
            this.val = val;
        }
        char val;      // root's val is (char) 0
        TrieNode fail; // root's fail is null
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isWord;
    }
}
