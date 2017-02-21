package com.ccnode.codegenerator.util;

import com.ccnode.codegenerator.dialog.MapperUtil;
import com.ccnode.codegenerator.dialog.datatype.ClassFieldInfo;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.PsiTypesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created by bruce.ge on 2016/12/9.
 */
public class PsiClassUtil {


    private static Map<String, String> primitiveToObjectMap = new HashMap<String, String>() {
        {
            put("int", "java.lang.Integer");
            put("short", "java.lang.Short");
            put("long", "java.lang.Long");
            put("double", "java.lang.Double");
            put("float", "java.lang.FLoat");
            put("boolean", "java.lang.Boolean");
            put("byte[]", "java.lang.Byte");
        }
    };

    public static List<String> extractProps(PsiClass pojoClass) {
        PsiField[] allFields = pojoClass.getAllFields();
        List<String> props = new ArrayList<String>();
        for (PsiField psiField : allFields) {
            if (isSupportedFieldType(psiField)) {
                props.add(psiField.getName());
            }
        }
        return props;
    }

    public static PsiMethod getAddMethod(PsiClass srcClass) {
        PsiMethod[] methods = srcClass.getMethods();
        List<PsiMethod> methodsList = new ArrayList<PsiMethod>();
        for (PsiMethod classMethod : methods) {
            String name = classMethod.getName().toLowerCase();
            if (name.startsWith("insert") || name.startsWith("save") || name.startsWith("add") || name.startsWith("create")) {
                if (classMethod.getParameterList().getParameters().length == 1) {
                    PsiParameter parameter = classMethod.getParameterList().getParameters()[0];
                    //mean is not system class like collection class ect.
                    if (!parameter.getType().getCanonicalText().startsWith("java.")) {
                        methodsList.add(classMethod);
                    }
                }
            }
        }
        if (methodsList.size() == 0) {
            return null;
        } else {
            PsiMethod miniMethod = methodsList.get(0);
            for (int i = 1; i < methodsList.size(); i++) {
                if (methodsList.get(i).getName().length() < miniMethod.getName().length()) {
                    miniMethod = methodsList.get(i);
                }
            }
            return miniMethod;
        }
    }

    public static PsiClass getPojoClass(PsiClass srcClass) {
        PsiMethod addMethod = getAddMethod(srcClass);
        if (addMethod != null) {
            PsiParameterList parameterList = addMethod.getParameterList();
            PsiParameter[] parameters = parameterList.getParameters();
            if (parameters.length == 1) {
                PsiType type = parameters[0].getType();
                PsiClass psiClass = PsiTypesUtil.getPsiClass(type);
                PsiField[] allFields = psiClass.getAllFields();
                //// TODO: 2016/12/15 maybe need check if exist id property.
                if (allFields != null && allFields.length > 0) {
                    return psiClass;
                }
                //try to get it from the class.
            }
        }
        return null;
    }

    public static List<ClassFieldInfo> buildPropFieldInfo(PsiClass psiClass) {
        List<ClassFieldInfo> lists = new ArrayList<>();
        PsiField[] allFields = psiClass.getAllFields();
        for (PsiField psiField : allFields) {
            if (isSupportedFieldType(psiField)) {
                ClassFieldInfo info = new ClassFieldInfo();
                info.setFieldName(psiField.getName());
                info.setFieldType(convertToObjectText(psiField.getType().getCanonicalText()));
                lists.add(info);
            }
        }
        return lists;
    }

    public static boolean isSupportedFieldType(PsiField psiField) {
        return psiField.hasModifierProperty("private") && !psiField.hasModifierProperty("static");
    }


    public static List<String> buildPropFields(PsiClass psiClass) {
        List<String> lists = new ArrayList<>();
        PsiField[] allFields = psiClass.getAllFields();
        for (PsiField psiField : allFields) {
            if (isSupportedFieldType(psiField)) {
                lists.add(psiField.getName());
            }
        }
        return lists;
    }

    @NotNull
    public static Map<String, String> buildFieldMapWithConvertPrimitiveType(PsiClass pojoClass) {
        Map<String, String> fieldMap = new HashMap<>();
        PsiField[] allFields = pojoClass.getAllFields();
        for (PsiField f : allFields) {
            if (isSupportedFieldType(f)) {
                String canonicalText = f.getType().getCanonicalText();
                String objectTypeText = convertToObjectText(canonicalText);
                fieldMap.put(f.getName(), objectTypeText);
            }
        }
        return fieldMap;
    }


    @NotNull
    public static Map<String, String> buildFieldMap(PsiClass pojoClass) {
        Map<String, String> fieldMap = new HashMap<>();
        PsiField[] allFields = pojoClass.getAllFields();
        for (PsiField f : allFields) {
            if (isSupportedFieldType(f)) {
                String canonicalText = f.getType().getCanonicalText();
                String objectTypeText = canonicalText;
                fieldMap.put(f.getName(), objectTypeText);
            }
        }
        return fieldMap;
    }

    public static String convertToObjectText(String canonicalText) {
        String s = primitiveToObjectMap.get(canonicalText);
        if (s != null) {
            return s;
        } else {
            return canonicalText;
        }
    }

    public static boolean isPrimitiveType(String type) {
        return primitiveToObjectMap.containsKey(type);
    }

    @Nullable
    public static PsiClass findClassOfQuatifiedType(PsiElement element, String resultTypeValue) {
        Module moduleForPsiElement =
                ModuleUtilCore.findModuleForPsiElement(element);
        if (moduleForPsiElement == null) {
            return null;
        }
        PsiClass[] classesByName = PsiShortNamesCache.getInstance(element.getProject()).getClassesByName(MapperUtil.extractClassShortName(resultTypeValue)
                , GlobalSearchScope.moduleScope(moduleForPsiElement));
        if (classesByName.length == 0) {
            return null;
        }

        for (PsiClass psiClass : classesByName) {
            if (psiClass.getQualifiedName().equals(resultTypeValue)) {
                return psiClass;
            }
        }
        return null;
    }

    @Nullable
    public static PsiClass findClassOfQuatifiedType(@Nullable Module module, @NotNull Project project, @NotNull String resultTypeValue) {
        if (module == null) {
            return null;
        }
        PsiClass[] classesByName = PsiShortNamesCache.getInstance(project).getClassesByName(MapperUtil.extractClassShortName(resultTypeValue)
                , GlobalSearchScope.moduleScope(module));
        if (classesByName.length == 0) {
            return null;
        }

        for (PsiClass psiClass : classesByName) {
            if (psiClass.getQualifiedName().equals(resultTypeValue)) {
                return psiClass;
            }
        }
        return null;
    }
}
