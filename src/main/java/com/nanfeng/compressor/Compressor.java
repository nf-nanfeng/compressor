package com.nanfeng.compressor;

import java.io.File;

/**
 * @author 楠枫
 */
public interface Compressor {

    /**
     * zip source to dest file (absolute fileName)
     *
     * @param src  src File
     * @param dest dest File
     */
    void zip(File src, File dest);

    /**
     * unzip source to dest file (absolute fileName)
     *
     * @param src  src File
     * @param dest dest File
     */
    void unzip(File src, File dest);

}
