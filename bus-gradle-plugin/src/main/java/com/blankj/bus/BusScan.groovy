package com.blankj.bus

import com.blankj.util.JavassistUtils
import com.blankj.util.LogUtils
import com.blankj.util.ZipUtils
import com.blankj.utilcode.util.BusUtils
import groovy.io.FileType
import javassist.CtClass
import javassist.CtMethod
import org.apache.commons.io.FileUtils

import java.lang.reflect.Modifier

class BusScan {

    HashMap<String, String> busMap
    List<File> scans
    File busJar

    BusScan() {
        busMap = [:]
        scans = []
    }

    void scanJar(File jar) {
        File tmp = new File(jar.getParent(), "temp_" + jar.getName())
        List<File> unzipFile = ZipUtils.unzipFile(jar, tmp)
        if (unzipFile != null && unzipFile.size() > 0) {
            scanDir(tmp)
            FileUtils.forceDelete(tmp)
        }
    }

    void scanDir(File root) {
        String rootPath = root.getAbsolutePath()
        if (!rootPath.endsWith(Config.FILE_SEP)) {
            rootPath += Config.FILE_SEP
        }

        if (root.isDirectory()) {
            root.eachFileRecurse(FileType.FILES) { File file ->
                def fileName = file.name
                if (!fileName.endsWith('.class')
                        || fileName.startsWith('R$')
                        || fileName == 'R.class'
                        || fileName == 'BuildConfig.class') {
                    return
                }

                def filePath = file.absolutePath
                def packagePath = filePath.replace(rootPath, '')
                def className = packagePath.replace(Config.FILE_SEP, ".")
                // delete .class
                className = className.substring(0, className.length() - 6)

                CtClass ctClass = JavassistUtils.getPool().get(className)

                CtMethod[] methods = ctClass.getDeclaredMethods();
                for (CtMethod method : methods) {
                    if (method.hasAnnotation(BusUtils.Subscribe)) {
                        if (Modifier.isStatic(method.getModifiers())) {
                            String name = method.getAnnotation(BusUtils.Subscribe).name()
                            String sign = method.getReturnType().getName() + ' ' + method.getLongName()
                            busMap.put(name, sign)
                        } else {
                            LogUtils.l(method.getLongName() + "is not static")
                        }
                    }
                }
            }
        }
    }
}
