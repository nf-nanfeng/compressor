package com.nanfeng.compressor;

import cn.hutool.core.io.IoUtil;
import cn.hutool.crypto.digest.MD5;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 楠枫
 */
public class HuffmanCompressor extends AbstractCompressor {

    @Override
    public void zip(File src, File dest) {
        byte[] bytes = readBytes(src);
        if (Objects.isNull(bytes)) {
            return;
        }

        HuffmanNode huffmanTree = buildHuffmanTree(bytes);
        HuffmanCompressObj compressObj = compress(huffmanTree, bytes);

        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(dest));
            oos.writeObject(compressObj);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            IoUtil.close(oos);
        }

        System.out.println("压缩率: " + ((bytes.length - compressObj.bytes.length) * 1.0 / bytes.length));
    }

    @Override
    public void unzip(File src, File dest) {
        HuffmanCompressObj compressObj = null;
        ObjectInputStream ois = null;

        try {
            ois = new ObjectInputStream(new FileInputStream(src));
            compressObj = (HuffmanCompressObj) ois.readObject();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            IoUtil.close(ois);
        }

        if (Objects.isNull(compressObj)) {
            System.out.println("File has been damaged !!! ");
            return;
        }

        byte[] bytes = expand(compressObj);
        writeBytes(bytes, dest);
    }

    private HuffmanNode buildHuffmanTree(byte[] bytes) {
        if (Objects.isNull(bytes) || bytes.length == 0) {
            return null;
        }

        Map<Byte, Integer> weightMap = new HashMap<>(256);
        for (byte b : bytes) {
            weightMap.put(b, weightMap.getOrDefault(b, 0) + 1);
        }

        List<HuffmanNode> nodes = weightMap.entrySet().stream().map(it -> new HuffmanNode(it.getKey(), it.getValue(), null, null)).collect(Collectors.toList());
        while (nodes.size() > 1) {
            nodes.sort(HuffmanNode::compareTo);
            HuffmanNode node1 = nodes.get(0), node2 = nodes.get(1);
            HuffmanNode node = new HuffmanNode(null, node1.weight + node2.weight, node1, node2);

            nodes.add(node);
            nodes.remove(node1);
            nodes.remove(node2);
        }

        return nodes.get(0);
    }

    private HuffmanCompressObj compress(HuffmanNode tree, byte[] bytes) {
        StringBuilder sb = new StringBuilder();

        Map<Byte, String> codeMap = new HashMap<>(256);
        for (byte b : bytes) {
            if (!codeMap.containsKey(b)) {
                codeMap.put(b, getCode(tree, b, new StringBuilder()));
            }

            sb.append(codeMap.get(b));
        }

        String code = sb.toString();
        int len = code.length();

        if (code.length() % 8 != 0) {
            code = code + "0".repeat(8 - code.length() % 8);
        }

        int total = code.length() / 8;
        byte[] result = new byte[total];

        for (int i = 0; i < total; i++) {
            String subCode = code.substring(i * 8, (i + 1) * 8);
            result[i] = bitStringToByte(subCode);
        }

        String md5 = MD5.create().digestHex(bytes);
        return new HuffmanCompressObj(len, md5, result, tree);
    }

    private byte[] expand(HuffmanCompressObj compressObj) {
        StringBuilder sb = new StringBuilder();
        for (byte b : compressObj.bytes) {
            sb.append(byteToBit(b));
        }

        String code = sb.toString();
        HuffmanNode tree = compressObj.huffmanTree;
        List<Byte> bytes = new ArrayList<>();

        for (int i = 0; i < compressObj.len; i++) {
            if (code.charAt(i) == '0') {
                tree = tree.left;
            } else {
                tree = tree.right;
            }

            if (tree.isLeaf()) {
                bytes.add(tree.data);
                tree = compressObj.huffmanTree;
            }
        }

        byte[] result = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            result[i] = bytes.get(i);
        }

        String md5 = MD5.create().digestHex(result);
        if (!StringUtils.equals(md5, compressObj.md5)) {
            System.out.println("File has been damaged !!! ");
            return null;
        }

        return result;
    }

    private String getCode(HuffmanNode root, Byte target, StringBuilder sb) {
        if (root.isLeaf()) {
            return Objects.equals(target, root.data) ? sb.toString() : null;
        }

        sb.append("0");
        String code = getCode(root.left, target, sb);
        sb.deleteCharAt(sb.length() - 1);

        if (StringUtils.isNotEmpty(code)) {
            return code;
        }

        sb.append("1");
        code = getCode(root.right, target, sb);
        sb.deleteCharAt(sb.length() - 1);

        return code;
    }

    private byte[] readBytes(File file) {
        InputStream ins = null;
        byte[] bytes = null;

        try {
            ins = new FileInputStream(file);
            bytes = new byte[ins.available()];
            int read = ins.read(bytes);
            if (read <= 0) {
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IoUtil.close(ins);
        }

        return bytes;
    }

    private void writeBytes(byte[] bytes, File file) {
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            os.write(bytes);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            IoUtil.close(os);
        }
    }

    @Data
    @RequiredArgsConstructor
    private static class HuffmanNode implements Serializable, Comparable<HuffmanNode> {

        private final Byte data;

        private final int weight;

        private final HuffmanNode left;

        private final HuffmanNode right;

        public boolean isLeaf() {
            return Objects.isNull(left) && Objects.isNull(right);
        }

        @Override
        public int compareTo(HuffmanNode o) {
            return this.weight - o.weight;
        }
    }

    @Data
    @RequiredArgsConstructor
    private static class HuffmanCompressObj implements Serializable {

        private final int len;

        private final String md5;

        private final byte[] bytes;

        private final HuffmanNode huffmanTree;

    }

}
