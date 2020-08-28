package com.usi.emadpres.parser.parser.ParserCore.utils;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

public final class BindingUtil {
	private BindingUtil() {
		// Empty constructor. Just to prevent instantiation.
	}

	public static String qualifiedName(IMethodBinding binding) {
		return qualifiedName(binding.getDeclaringClass()) + "." + binding.getName();
	}

	public static String qualifiedSignature(IMethodBinding binding) {
		StringBuffer buf = new StringBuffer();
		if (binding != null) {
			buf.append(qualifiedName(binding.getDeclaringClass())).append('.').append(binding.getName()).append('(');
			ITypeBinding[] parameterTypes = binding.getParameterTypes();
			for (int i = 0; i < parameterTypes.length; i++) {
				if (i != 0) {
					buf.append(',');
				}
				buf.append(qualifiedName(parameterTypes[i]));
			}
			buf.append(')');
		}
		return buf.toString();
	}

	public static String qualifiedName(final ITypeBinding binding) {
		return binding.getTypeDeclaration().getQualifiedName();
	}

	public static String qualifiedName(IVariableBinding binding) {
		ITypeBinding declaringClass = binding.getDeclaringClass();

		if (null == declaringClass) {
			return binding.getName();
		}

		return qualifiedName(declaringClass) + "." + binding.getName();
	}
}
