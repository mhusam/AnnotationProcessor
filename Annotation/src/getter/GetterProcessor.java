package getter;

import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import java.util.Set;
import java.util.stream.IntStream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes("getter.Getter")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class GetterProcessor extends AbstractProcessor {

    private Trees trees;
    private TreeTranslator visitor;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();

        trees = Trees.instance(processingEnv);
        visitor = new GetterTranslator(context);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        if (!roundEnvironment.processingOver()) {
            Set<? extends Element> elements = roundEnvironment.getRootElements();
            elements.forEach(element -> {
                JCTree tree = (JCTree) trees.getTree(element);
                tree.accept(visitor);
            });
        } else {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "No assertions inlined.");
        }
        return false;
    }

    class GetterTranslator extends TreeTranslator {

        private final TreeMaker maker;
        private final JavacElements elements;

        public GetterTranslator(Context context) {
            this.maker = TreeMaker.instance(context);
            this.elements = JavacElements.instance(context);
        }

        @Override
        public void visitMethodDef(JCMethodDecl methodDecl) {
            super.visitMethodDef(methodDecl);

            boolean hasTargetAnnotation = methodDecl.mods.annotations.stream().anyMatch(e -> e.getAnnotationType().type.toString().equals(Getter.class.getName()));
            if(hasTargetAnnotation) {
                
                // Get Annotation Args
//                JCTree.JCAnnotation annotation = methodDecl.mods.annotations.stream()
//                    .filter(e -> e.getAnnotationType().type.toString().equals(Getter.class.getName()))
//                    .findFirst().get(/*always present*/);
//                List args = (List) annotation.attribute.values.get(0).snd.getValue();
                
                
                
                JCBlock body = createBody(methodDecl.body);

                result = maker.MethodDef(methodDecl.mods, methodDecl.name,
                        methodDecl.restype, methodDecl.typarams,
                        methodDecl.params, methodDecl.thrown,
                        body, methodDecl.defaultValue);
            }
        }

        private JCBlock createBody(JCBlock oldBody) {
            ListBuffer<JCTree.JCStatement> list = new ListBuffer<>();

            list.add(println(maker.Literal("Start")));
            
            IntStream.range(0, oldBody.stats.size()).forEach((int idx) -> {
                list.add(oldBody.stats.get(idx));
            });
            
            list.add(println(maker.Literal("End")));

            return maker.Block(0, list.toList());
        }

        private JCTree.JCStatement println(JCTree.JCExpression exp) {
            JCTree.JCExpression method = maker.Select(
                    maker.Select(
                            maker.QualIdent(getClassSymbol(System.class)), 
                            elements.getName("out")),  
                    elements.getName("println"));

            JCTree.JCMethodInvocation invocation = maker.Apply(List.nil(), method, List.of(exp));
            return maker.Exec(invocation);
        }

        private Symbol.ClassSymbol getClassSymbol(Class<?> clazz) {
            return elements.getTypeElement(clazz.getName());
        }
    }
}
