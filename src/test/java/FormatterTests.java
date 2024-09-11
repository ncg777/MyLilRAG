import junit.framework.TestCase;

public class FormatterTests extends TestCase {

    protected void setUp() throws Exception {
	super.setUp();
    }

    protected void tearDown() throws Exception {
	super.tearDown();
    }

    public void testFormatAnswer()
    {
	Formatter.lineLength = 80;
	String result = Formatter.formatAnswer("the elements of a set in a particular order. For example, if we have a set of three elements {a, b, c}, the permutations of this set are:");
	String expected =	      "the elements of a set in a particular order. For example, if we have a set of\nthree elements {a, b, c}, the permutations of this set are:";
        assertEquals(expected, result);
        
        result = Formatter.formatAnswer("skhfkh slsg slkjglsjdlg sldjglsjglk sljglsjg sldjglsjdlg sldjglsdgl {abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefgh}");
	expected =	               "skhfkh slsg slkjglsjdlg sldjglsjglk sljglsjg sldjglsjdlg sldjglsdgl\n{abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefgh}";
        assertEquals(expected, result);
        
        
        result = Formatter.formatAnswer("A combination is a selection of objects from a collection where the order doesn't matter. In other words, it is a way of choosing elements from a set in which the order of selection does not affect the outcome.");
        expected = "A combination is a selection of objects from a collection where the order\ndoesn't matter. In other words, it is a way of choosing elements from a set in\nwhich the order of selection does not affect the outcome.";
        assertEquals(expected, result);
        
        result = Formatter.formatAnswer("abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg -abcdefgh test");
	expected =                     "abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg\n-abcdefgh test";
        assertEquals(expected, result);
        
        result = Formatter.formatAnswer("abcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefgh-abcdefgh");
	expected =                     "abcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefgh-abcdefg\nh";
        assertEquals(expected, result);
    }

}
