package cn.kivensoft.util;

/** 元数据对象类, 提供1-6个参数的接口实现类
 * @author kiven lee
 * @version 1.0
 * @date 2020-01-05
 */
final public class Tuple  {
	
	private Tuple() { }

	public interface T1<E1> {
		E1 arg1();
		T1<E1> arg1(E1 value);
	}

	public interface T2<E1, E2> extends T1<E1> {
		@Override
		T2<E1, E2> arg1(E1 value);
		E2 arg2();
		T2<E1, E2> arg2(E2 value);
	}

	public interface T3<E1, E2, E3> extends T2<E1, E2> {
		@Override
		T3<E1, E2, E3> arg1(E1 value);
		@Override
		T3<E1, E2, E3> arg2(E2 value);
		E3 arg3();
		T3<E1, E2, E3> arg3(E3 value);
	}

	public interface T4<E1, E2, E3, E4> extends T3<E1, E2, E3> {
		@Override
		T4<E1, E2, E3, E4> arg1(E1 value);
		@Override
		T4<E1, E2, E3, E4> arg2(E2 value);
		@Override
		T4<E1, E2, E3, E4> arg3(E3 value);
		E4 arg4();
		T4<E1, E2, E3, E4> arg4(E4 value);
	}

	public interface T5<E1, E2, E3, E4, E5> extends T4<E1, E2, E3, E4> {
		@Override
		T5<E1, E2, E3, E4, E5> arg1(E1 value);
		@Override
		T5<E1, E2, E3, E4, E5> arg2(E2 value);
		@Override
		T5<E1, E2, E3, E4, E5> arg3(E3 value);
		@Override
		T5<E1, E2, E3, E4, E5> arg4(E4 value);
		E5 arg5();
		T5<E1, E2, E3, E4, E5> arg5(E5 value);
	}

	public interface T6<E1, E2, E3, E4, E5, E6> extends T5<E1, E2, E3, E4, E5> {
		@Override
		T6<E1, E2, E3, E4, E5, E6> arg1(E1 value);
		@Override
		T6<E1, E2, E3, E4, E5, E6> arg2(E2 value);
		@Override
		T6<E1, E2, E3, E4, E5, E6> arg3(E3 value);
		@Override
		T6<E1, E2, E3, E4, E5, E6> arg4(E4 value);
		@Override
		T6<E1, E2, E3, E4, E5, E6> arg5(E5 value);
		E6 arg6();
		T6<E1, E2, E3, E4, E5, E6> arg6(E6 value);
	}

	final public static <E1, E2> T2<E1, E2> of(E1 arg1, E2 arg2) {
		
		return new T2<E1, E2>() {
			private E1 v1 = arg1;
			private E2 v2 = arg2;

			@Override
			public final E1 arg1() { return v1; }
			@Override
			public final T2<E1, E2> arg1(E1 value) { v1 = value; return this; }

			@Override
			public final E2 arg2() { return v2; }
			@Override
			public final T2<E1, E2> arg2(E2 value) { v2 = value; return this; }
		};
	}

	final public static <E1, E2, E3> T3<E1, E2, E3> of(E1 arg1, E2 arg2, E3 arg3) {
		return new T3<E1, E2, E3>() {
			private E1 v1 = arg1;
			private E2 v2 = arg2;
			private E3 v3 = arg3;

			@Override
			public final E1 arg1() { return v1; }
			@Override
			public final T3<E1, E2, E3> arg1(E1 value) { v1 = value; return this; }

			@Override
			public final E2 arg2() { return v2; }
			@Override
			public final T3<E1, E2, E3> arg2(E2 value) { v2 = value; return this; }

			@Override
			public final E3 arg3() { return v3; }
			@Override
			public final T3<E1, E2, E3> arg3(E3 value) { v3 = value; return this; }
		};
	}

	final public static <E1, E2, E3, E4> T4<E1, E2, E3, E4> of(E1 arg1, E2 arg2, E3 arg3, E4 arg4) {
		return new T4<E1, E2, E3, E4>() {
			private E1 v1 = arg1;
			private E2 v2 = arg2;
			private E3 v3 = arg3;
			private E4 v4 = arg4;

			@Override
			public final E1 arg1() { return v1; }
			@Override
			public final T4<E1, E2, E3, E4> arg1(E1 value) { v1 = value; return this; }

			@Override
			public final E2 arg2() { return v2; }
			@Override
			public final T4<E1, E2, E3, E4> arg2(E2 value) { v2 = value; return this; }

			@Override
			public final E3 arg3() { return v3; }
			@Override
			public final T4<E1, E2, E3, E4> arg3(E3 value) { v3 = value; return this; }

			@Override
			public final E4 arg4() { return v4; }
			@Override
			public final T4<E1, E2, E3, E4> arg4(E4 value) { v4 = value; return this; }
		};
	}

	final public static <E1, E2, E3, E4, E5> T5<E1, E2, E3, E4, E5> of(
			E1 arg1, E2 arg2, E3 arg3, E4 arg4, E5 arg5) {
		return new T5<E1, E2, E3, E4, E5>() {
			private E1 v1 = arg1;
			private E2 v2 = arg2;
			private E3 v3 = arg3;
			private E4 v4 = arg4;
			private E5 v5 = arg5;

			@Override
			public E1 arg1() { return v1; }
			@Override
			public T5<E1, E2, E3, E4, E5> arg1(E1 value) { v1 = value; return this; }

			@Override
			public E2 arg2() { return v2; }
			@Override
			public T5<E1, E2, E3, E4, E5> arg2(E2 value) { v2 = value; return this; }

			@Override
			public E3 arg3() { return v3; }
			@Override
			public T5<E1, E2, E3, E4, E5> arg3(E3 value) { v3 = value; return this; }

			@Override
			public E4 arg4() { return v4; }
			@Override
			public T5<E1, E2, E3, E4, E5> arg4(E4 value) { v4 = value; return this; }

			@Override
			public E5 arg5() { return v5; }
			@Override
			public T5<E1, E2, E3, E4, E5> arg5(E5 value) { v5 = value; return this; }
		};
	}

	final public static <E1, E2, E3, E4, E5, E6> T6<E1, E2, E3, E4, E5, E6> make(
			E1 arg1, E2 arg2, E3 arg3, E4 arg4, E5 arg5, E6 arg6) {
		return new T6<E1, E2, E3, E4, E5, E6>() {
			private E1 v1 = arg1;
			private E2 v2 = arg2;
			private E3 v3 = arg3;
			private E4 v4 = arg4;
			private E5 v5 = arg5;
			private E6 v6 = arg6;

			@Override
			public E1 arg1() { return v1; }
			@Override
			public T6<E1, E2, E3, E4, E5, E6> arg1(E1 value) { v1 = value; return this; }

			@Override
			public E2 arg2() { return v2; }
			@Override
			public T6<E1, E2, E3, E4, E5, E6> arg2(E2 value) { v2 = value; return this; }

			@Override
			public E3 arg3() { return v3; }
			@Override
			public T6<E1, E2, E3, E4, E5, E6> arg3(E3 value) { v3 = value; return this; }

			@Override
			public E4 arg4() { return v4; }
			@Override
			public T6<E1, E2, E3, E4, E5, E6> arg4(E4 value) { v4 = value; return this; }

			@Override
			public E5 arg5() { return v5; }
			@Override
			public T6<E1, E2, E3, E4, E5, E6> arg5(E5 value) { v5 = value; return this; }

			@Override
			public E6 arg6() { return v6; }
			@Override
			public T6<E1, E2, E3, E4, E5, E6> arg6(E6 value) { v6 = value; return this; }
		};
	}

}
