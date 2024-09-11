import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class MyLilRAGTests 
    extends TestCase
{
    public MyLilRAGTests( String testName )
    {
        super( testName );
    }

    public static Test suite()
    {
        return new TestSuite( MyLilRAGTests.class );
    }

    public void testFormatAnswer()
    {
	String result = MyLilRAG.formatAnswer("the elements of a set in a particular order. For example, if we have a set of three elements {a, b, c}, the permutations of this set are:");
	String expected =	      "the elements of a set in a particular order. For example, if we have a set of\nthree elements {a, b, c}, the permutations of this set are:";
        assertEquals(expected, result);
        
        result = MyLilRAG.formatAnswer("A combination is a selection of objects from a collection where the order doesn't matter. In other words, it is a way of choosing elements from a set in which the order of selection does not affect the outcome.");
        expected = "A combination is a selection of objects from a collection where the order\ndoesn't matter. In other words, it is a way of choosing elements from a set in\nwhich the order of selection does not affect the outcome.";
        assertEquals(expected, result);
        
        result = MyLilRAG.formatAnswer("abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg -abcdefgh test");
	expected =                     "abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg\n-abcdefgh test";
        assertEquals(expected, result);
        
        result = MyLilRAG.formatAnswer("abcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefgh-abcdefgh");
	expected =                     "abcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefgh-abcdefg\nh";
        assertEquals(expected, result);
    }
}
