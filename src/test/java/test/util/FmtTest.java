package test.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.kivensoft.util.Fmt;
import cn.kivensoft.util.Strings;

public class FmtTest {
	public static class A {
		public Integer age;
		private String name;
		public String getName() { return name; }
		public void setName(String name) { this.name = name; }
		public A() {}
		public A(Integer age, String name) {
			this.age = age;
			this.name = name;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((age == null) ? 0 : age.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			A other = (A) obj;
			if (age == null) {
				if (other.age != null)
					return false;
			} else if (!age.equals(other.age))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
	}
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testFormat() {
		System.out.println();
		Date d = Strings.parseDate("2004-1-3 4:5:6");
		int[] ints = {3, 5, 9};
		Integer[] Ints = {3, 5, 9};
		List<Integer> list = Arrays.asList(Ints);
		assertEquals("load 2004-01-03 04:05:06 ok", Fmt.fmt("load {} ok", d));
		assertEquals("2004-01-03 04:05:06 3,5,9 {}", Fmt.fmt("{} {} {}", d, ints));
		assertEquals("{} 3,5,9", Fmt.fmt("\\{} {}", ints));
		assertEquals("2004-01-03 04:05:06 3,5,9 {}", Fmt.fmt("{} {} {}",
				new Object[] {d, ints}));
		assertEquals("{} 3,5,9", Fmt.fmt("\\{} {}", new Object[] {ints}));
		assertEquals("[3, 5, 9]", Fmt.get().appendPrimitiveArray(
				ints, ", ", "[", "]").release());
		assertEquals("[3, 5, 9]", Fmt.get().append(Ints, ", ", "[", "]").release());
		assertEquals("3,5,9", Fmt.fmt("{}", list));
		assertEquals("3,5,9", Fmt.fmt("{},{},{}", (f, i) -> f.append(list.get(i))));
		assertEquals("{3,5,9{", Fmt.fmt("{{}{", list));
	}
	
	@Test
	public void testJson() {
		A a = new A(33, "hello");
		Integer[] Ints = {3, 5, 9};
		assertEquals("{\"age\": 33, \"name\": \"hello\"}", Fmt.toJson(a));
		assertEquals("{\"age\": 33, \"name\": \"hello\"}", Fmt.fmtJson("{}", a));
		assertEquals("{\"age\": 33, \"name\": \"hello\"}, [3, 5, 9], 42",
				Fmt.fmtJson("{}, {}, {}", a, Ints, 42));
	}
	
	@Test
	public void testOpera() throws Exception {
		assertEquals("abcdefg", Fmt.concat("abc", "def", "g"));
		assertEquals("abcdefg", Fmt.concat(new String[] {"abc", "def", "g"}));
		assertEquals("abcdefg1234", Fmt.concat("abc", "def", "g", 12, 34));
		assertEquals("abcdefg1234", Fmt.concat(new Object[] {"abc", "def", "g", 12, 34}));
		
		assertEquals("--------", Fmt.rep('-', 8));
		assertEquals("-=-=-=-=-=-=-=-", Fmt.get().repeat("-", 8, "=").release());
		
		assertEquals("f2345678", Fmt.get().appendHex(0xF2345678).release());
		//assertEquals("f2345678-a8765432", Fmt.toHex('-', 0xF2345678, 0xa8765432));
		assertEquals("f2345678a8765432", Fmt.get().appendHex(0xF2345678a8765432L).release());
		assertEquals("f2-34-56-78", Fmt.get().appendHex(
				new byte[] {(byte)0xF2, 0x34, 0x56, 0x78}, '-').release());
		
		assertEquals("5Y+j5Luk", Fmt.get().appendBase64("口令".getBytes("utf8"), false).release());
		assertEquals("5Y+j5LukMQ==", Fmt.get().appendBase64("口令1".getBytes("utf8"), false).release());
		assertEquals("5Y+j5LukMTI=", Fmt.get().appendBase64("口令12".getBytes("utf8"), false).release());
		
		assertEquals("0xf2345678, 5Y+j5LukMQ==, -=0-=0-=0-=0-=0-=0-=0-",
				Fmt.fmt("0x{}, {}, {}", (f, i) -> {
			switch (i) {
				case 0: f.appendHex(0xF2345678); break;
				case 1: f.appendBase64(Strings.toBytes("口令1"), false); break;
				case 2: f.repeat("-", 8, "=0"); break;
			}
		}));
		
		assertEquals(Fmt.fmt("0123456").substring(1, 4), "123");
	}

	@Test
	public void testAppendUtf8() {
		String s = "ab中文";
		byte[] bs = Strings.toBytes(s);
		byte[] b1 = new byte[3];
		byte[] b2 = new byte[bs.length - 3];
		System.arraycopy(bs, 0, b1, 0, 3);
		System.arraycopy(bs, 3, b2, 0, b2.length);
		String s2 = Fmt.get().append(bs).release();
		assertEquals(s, s2);
	}
	
	@Test
	public void testHex() {
		byte[] bs = {1, 2, 3, 80, 81, 127, (byte)128, (byte)129, (byte)254, (byte)255};
		String r = "01020350517f8081feff";
		assertEquals(r, Fmt.get().appendHex(bs).release());
		assertEquals("01 02 03 50 51 7f 80 81 fe ff", Fmt.get().appendHex(bs, ' ').release());
		assertEquals("null", Fmt.get().appendHex((byte[])null).release());
	}

	@Test
	public void testAppendable() {
		StringBuilder sb = new StringBuilder();
		for (int i = 100000; i < 109999; i++) sb.append(i).append(',');
		Fmt f = Fmt.get();
		f.append(sb);
		assertEquals(sb.toString(), f.toString());
	}

	@Test
	public void testAppendFmt() {
		Fmt sb = Fmt.get();
		for (int i = 100000; i < 100999; i++) sb.append(i).append(',');
		Fmt f = Fmt.get();
		f.append(sb);
		assertEquals(sb.toString(), f.toString());
	}
}

