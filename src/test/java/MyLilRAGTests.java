import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class MyLilRAGTests 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public MyLilRAGTests( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( MyLilRAGTests.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testFormatAnswer()
    {
	String result = MyLilRAG.formatAnswer("abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg {abcdefg}");
	String expected = "abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg\n{abcdefg}";
        assertEquals(expected, result);
        
        result = MyLilRAG.formatAnswer("abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg -abcdefgh test");
	expected =                     "abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg abcdefg\n-abcdefgh test";
        assertEquals(expected, result);
        result = MyLilRAG.formatAnswer("abcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefgh-abcdefgh");
	expected =                     "abcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefgh-abcdefg\nh";
        assertEquals(expected, result);
    }
}
