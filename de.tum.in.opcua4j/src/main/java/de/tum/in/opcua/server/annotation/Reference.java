package de.tum.in.opcua.server.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.tum.in.opcua.server.ReferenceType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Reference {

	ReferenceType refType() default ReferenceType.hasComponent;

	String displayName() default "";
}
