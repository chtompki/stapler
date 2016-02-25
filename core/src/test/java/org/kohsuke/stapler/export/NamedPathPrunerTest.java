package org.kohsuke.stapler.export;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;

public class NamedPathPrunerTest extends TestCase {

    private static ExportConfig config = new ExportConfig().withClassAttribute(ClassAttributeBehaviour.IF_NEEDED.simple());

    public NamedPathPrunerTest(String name) {
        super(name);
    }
    
    public void testParse() throws Exception {
        assertEquals("{a={}, b={c={}}}", NamedPathPruner.parse("a,b[c]").toString());
        assertEquals("{a={}, b={c={}, d={}}}", NamedPathPruner.parse("a,b[c,d]").toString());
        assertEquals("{a={}, b={c={}, d={}}, e={}}", NamedPathPruner.parse("a,b[c,d],e").toString());
        assertEquals("{a={}, b={c={}, d={}}, e={}}", NamedPathPruner.parse("a,b[c,d]{,10},e").toString());
        assertParseError("");
        assertParseError("a,");
        assertParseError(",b");
        assertParseError("a[");
        assertParseError("a[b,c");
        assertParseError("a[]");
        assertParseError("a[b,,]");
        assertParseError("a]");
        assertParseError("a{}");
        assertParseError("a{b}");
    }
    private static void assertParseError(String spec) {
        try {
            NamedPathPruner.parse(spec);
            fail();
        } catch (IllegalArgumentException x) {
            // pass
        }
    }
    
    public void testPruning() throws Exception {
        Jhob job1 = new Jhob("job1", "Job #1", "whatever");
        Jhob job2 = new Jhob("job2", "Job #2", "junk");
        Vhew view1 = new Vhew("All", "crap", new Jhob[] {job1, job2});
        Vhew view2 = new Vhew("Some", "less", new Jhob[] {job1});
        Stuff bean = new Stuff(new Jhob[] {job1, job2}, Arrays.asList(view1, view2));
        assertResult("{_class:Stuff,jobs:[{_class:Jhob,displayName:Job #1,name:job1},{_class:Jhob,displayName:Job #2,name:job2}],"
                + "views:[{_class:Vhew,jobs:[{_class:Jhob,name:job1},{_class:Jhob,name:job2}],name:All},{_class:Vhew,jobs:[{_class:Jhob,name:job1}],name:Some}]}",
                bean, "jobs[name,displayName],views[name,jobs[name]]");
        assertResult("{_class:Stuff,jobs:[{_class:Jhob,displayName:Job #1,name:job1}],views:[{_class:Vhew,jobs:[],name:All},"
                + "{_class:Vhew,jobs:[],name:Some}]}",
                bean, "jobs[name,displayName]{,1},views[name,jobs[name]{,0}]");
    }

    public void testRange() throws Exception {
        Jhob[] jobs = new Jhob[100];
        for (int i=0; i<jobs.length; i++)
            jobs[i] = new Jhob("job"+i,"aaa","bbb");
        Vhew v = new Vhew("view","aaa",jobs);

        assertResult("{_class:Vhew,jobs:[{_class:Jhob,name:job0},{_class:Jhob,name:job1},{_class:Jhob,name:job2}]}",    v, "jobs[name]{,3}");
        assertResult("{_class:Vhew,jobs:[{_class:Jhob,name:job3},{_class:Jhob,name:job4},{_class:Jhob,name:job5}]}",    v, "jobs[name]{3,6}");
        assertResult("{_class:Vhew,jobs:[{_class:Jhob,name:job38}]}",                           v, "jobs[name]{38}");
        assertResult("{_class:Vhew,jobs:[{_class:Jhob,name:job97},{_class:Jhob,name:job98},{_class:Jhob,name:job99}]}", v, "jobs[name]{97,}");
    }
    
    @ExportedBean public static class Stuff {
        @Exported public Jhob[] jobs;
        @Exported public List<Vhew> views;
        public Stuff(Jhob[] jobs, List<Vhew> views) {
            this.jobs = jobs.clone();
            this.views = views;
        }
    }
    @ExportedBean public static class Jhob {
        @Exported public String name;
        @Exported public String displayName;
        @Exported public String trash;
        public Jhob(String name, String displayName, String trash) {
            this.name = name;
            this.displayName = displayName;
            this.trash = trash;
        }
    }
    @ExportedBean public static class Vhew {
        @Exported public String name;
        @Exported public String trash;
        @Exported public Jhob[] jobs;
        public Vhew(String name, String trash, Jhob[] jobs) {
            this.name = name;
            this.trash = trash;
            this.jobs = jobs.clone();
        }
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"}) // API design flaw prevents this from type-checking
    private static void assertResult(String expected, Object bean, String spec) throws Exception {
        Model model = new ModelBuilder().get(bean.getClass());
        StringWriter w = new StringWriter();
        model.writeTo(bean, new NamedPathPruner(spec), Flavor.JSON.createDataWriter(bean, w, config));
        assertEquals(expected, w.toString().replace("\\\"", "").replace("\"", ""));
    }
    
}
