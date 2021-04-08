package com.github.eklir.androidprocessor.bindknife.proccessor;

import com.github.eklir.androidproccessor.bindknife.annotation.AutoBind;
import com.github.eklir.androidproccessor.bindknife.constants.BindKnifeConstants;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;


@AutoService({Processor.class})
public class BindProccessor extends AbstractProcessor {
    private static final String TAG = "BindKnife";
    private Map<TypeElement, List<Element>> parentAndChildren = new HashMap<>();
    private Filer filer;
    private Elements elementUtils;
    private Messager messager;
    private Types typeUtils;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> retVal = new HashSet<>();
        retVal.add("com.github.eklir.androidproccessor.bindknife.annotation.AutoBind");
        return retVal;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
        elementUtils = processingEnvironment.getElementUtils();
        messager = processingEnvironment.getMessager();
        typeUtils = processingEnvironment.getTypeUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Set<? extends Element> autoBindElements = roundEnvironment.getElementsAnnotatedWith(AutoBind.class);
        if (autoBindElements == null || autoBindElements.isEmpty()) {
            return false;
        }
        try {
            categories(autoBindElements);
            generateHelper();
        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "generateHelper failed:"+ e.getMessage());
        }
        return true;
    }

    private void generateHelper() throws IOException {
        if (!parentAndChildren.isEmpty()) {
            Set<Map.Entry<TypeElement, List<Element>>> entries = parentAndChildren.entrySet();
            for (Map.Entry<TypeElement, List<Element>> entry : entries) {
                //class
                TypeElement parent = entry.getKey();
                //filed
                List<Element> fileds = entry.getValue();
                String parentName = parent.getQualifiedName().toString();
                int lastDotIndex = parentName.lastIndexOf('.');
                String packageName = parentName.substring(0, lastDotIndex);
                //定义方法的参数
                ParameterSpec paramsSpec = ParameterSpec.builder(TypeName.OBJECT, "target").build();
                //定义方法体
                MethodSpec.Builder injectMethodBuilder = MethodSpec.methodBuilder("inject")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeName.VOID)
                        .addParameter(paramsSpec)
                        .addStatement("$T act = ($T) target", ClassName.get(parent), ClassName.get(parent));
                //对所有的字段赋值
                for (Element filed : fileds) {
                    TypeMirror typeMirror = filed.asType();
                    String fieldName = filed.getSimpleName().toString();
                    AutoBind annotation = filed.getAnnotation(AutoBind.class);
                    injectMethodBuilder.addStatement("act." + fieldName + " = ($T)act.findViewById(" + annotation.value() + ")", ClassName.get(typeMirror));
                }
                MethodSpec injectMethodSpec = injectMethodBuilder.build();
                //定义类文件
                TypeSpec helper = TypeSpec.classBuilder(parent.getSimpleName() + BindKnifeConstants.BIND_SUFFIX)
                        .addSuperinterface(ClassName.get("com.github.eklir.android.bindknife", "IKnife"))
                        .addMethod(injectMethodSpec)
                        .build();
                //生成类文件
                JavaFile.builder(packageName, helper).build().writeTo(filer);
            }
        }

    }

    /**
     * 按类对注解的字段分组
     *
     * @param autoBindElements
     * @throws IllegalAccessException
     */
    private void categories(Set<? extends Element> autoBindElements) throws IllegalAccessException {
        for (Element element : autoBindElements) {
            //字段的封装类型是类；
            TypeElement parent = (TypeElement) element.getEnclosingElement();
            if (element.getModifiers().contains(Modifier.PRIVATE)) {
                throw new IllegalAccessException("AutoBind 注解的字段不能为private！");
            }
            if (parentAndChildren.containsKey(parent)) {
                parentAndChildren.get(parent).add(element);
            } else {
                List<Element> eleList = new ArrayList<>();
                eleList.add(element);
                parentAndChildren.put(parent, eleList);
            }
        }
    }
}
