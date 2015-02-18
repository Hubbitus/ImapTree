package configuration.operations

import groovyx.javafx.GroovyFX
import info.hubbitus.imaptree.ImapTreeSize
import info.hubbitus.imaptree.config.Operation
import javafx.beans.property.ReadOnlyLongWrapper
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.scene.control.Label
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeTableColumn
import javafx.scene.control.TreeTableView
import javafx.scene.layout.StackPane

/**
 * Task to present gathered Imap-tree sizes as FX TreeTableView (
 * 	https://docs.oracle.com/javase/8/javafx/user-interface-tutorial/tree-table-view.htm
 * 	,https://wikis.oracle.com/display/OpenJDK/TreeTableView+User+Experience+Documentation)
 *
 * FX is yong but very powerful!
 * Even in that very simple operation representation you may select desired set of columns, sort, fold/unfold
 * sub-trees, select lines and so on. I think it is the best instrument just for investigation like "where my space"
 *
 * 	Unfortunately there also some quirks:
 * 	1) JavaFX is not fully and true open source yet. Most sources in http://openjdk.java.net/projects/openjfx/,
 * 	but included in OpenJDK version does not work for me. Oracle JDK does.
 * 	2) There used modern TreeTableView component, which is implemented only in Java-8. So only Oracle Java 8 is
 * 	now single choose to use it.
 * 	3) Even in Oracle JDK JavaFX still treated as non-mainstream, so not included in main classpath (placed in ext)
 * 	and for run such operation you may try:
 * 	3.1) Add it by run simple command as described: http://zenjava.com/javafx/maven/fix-classpath.html
 * 		(http://stackoverflow.com/questions/14095430/how-to-grab-a-dependency-and-make-it-work-with-intellij-project)
 * 	3.2) Or manually provide path to it on script running time:
 * 	$ groovy -cp $JAVA_HOME/jre/lib/ext/jfxrt.jar:/home/pasha/.groovy/grapes/org.codehaus.groovyfx/groovyfx/jars/groovyfx-0.4.0.jar ./ImapTree.groovy --account BackupTest --operation fxTreeTable -c
 * 	4) In that project also GroovyFX used, and due to reported by me bug:
 * 	https://jira.codehaus.org/browse/GFX-41 we can't automatically Grab 0.4.0 version which is required. As
 * 	workaround also you may provide path to .jar file manually as shown before.
 * I hope such issues will ba vanished in time.
 * Until it is not, and to do not make such dependencies mandatory, task written to use anywhere fully qualified
 * class names to avoid imports, which may lead to just do not start even other tasks!
 */
fxTreeTable = new Operation(
	fullControl: { ImapTreeSize imapTree ->
		// Runtime manually grab to do not make it hard dependency. All classes use full qualified names and
		// does not imported for that reason
//				@Grab(group = 'org.codehaus.groovyfx', module = 'groovyfx', version = '0.3.1')
		// Until BUG https://jira.codehaus.org/browse/GFX-41 resolved not 0.4.0 version
//				groovy.grape.Grape.grab(group: 'org.codehaus.groovyfx', module:'groovyfx', version:'0.3.1')
		GroovyFX.start {
			stage(title: 'IMAP Tree folder sizes', visible: true) {
				scene(fill: BLACK, width: 700, height: 300) {
					// Unfortunately GroovyFX have no TreeTableView builder (yet?), so build it manually and insert as node
					node new TreeTableView(new TreeItem(imapTree.tree)).with {
						it.root.expanded = true;
						it.tableMenuButtonVisible = true;

						imapTree.tree.@fxItem = it.root;
						imapTree.tree.depthFirst().tail().each { Node n -> // Except 1st root
							n.@fxItem = new TreeItem(n); // Store for childs
							n.parent().@fxItem.children.add(n.@fxItem);
						}

						it.columns.setAll(
								new TreeTableColumn('Folder name').with {
//										it.setPrefWidth(150);
									it.cellValueFactory = { TreeTableColumn.CellDataFeatures<Node, String> param ->
										new ReadOnlyStringWrapper(param.value.value.@folder.name) // Size object
									}
									it
								}
								, new javafx.scene.control.TreeTableColumn('Folder full name').with {
//										it.setPrefWidth(150);
							it.cellValueFactory = { TreeTableColumn.CellDataFeatures<Node, String> param ->
								new ReadOnlyStringWrapper(param.value.value.name()) // Size object
							}
							it
						}
								, new TreeTableColumn('Self size, bytes').with {
//										it.setPrefWidth(150);
							it.cellValueFactory = { TreeTableColumn.CellDataFeatures<Node, String> param ->
								new ReadOnlyLongWrapper(param.value.value.@size.bytes ?: 0) // Size object
							}
							it
						}
								, new TreeTableColumn('Self size, hr').with {
//										it.setPrefWidth(150);
							it.cellValueFactory = { TreeTableColumn.CellDataFeatures<Node, String> param ->
								new ReadOnlyStringWrapper(param.value.value.@size.hr()) // Size object
							}
							it
						}
								, new TreeTableColumn('Messages').with {
//										it.setPrefWidth(150);
							it.cellValueFactory = { TreeTableColumn.CellDataFeatures<Node, String> param ->
								new ReadOnlyLongWrapper(param.value.value.@size.messages ?: 0) // Size object
							}
							it
						}
								, new TreeTableColumn('Sub-tree size, bytes').with {
//										it.setPrefWidth(150);
							it.cellValueFactory = { TreeTableColumn.CellDataFeatures<Node, String> param ->
								new ReadOnlyLongWrapper(param.value.value.depthFirst().sum {
									it.@size
								}.bytes ?: 0) // Size object
							}
							it
						}
								, new TreeTableColumn('Sub-tree size, hr').with {
//										it.setPrefWidth(150);
							it.cellValueFactory = { TreeTableColumn.CellDataFeatures<Node, String> param ->
								new ReadOnlyStringWrapper(param.value.value.depthFirst().sum {
									it.@size
								}.hr()) // Size object
							}
							it
						}
								, new TreeTableColumn('Sub-tree messages').with {
//										it.setPrefWidth(150);
							it.cellValueFactory = { TreeTableColumn.CellDataFeatures<Node, String> param ->
								new ReadOnlyLongWrapper(param.value.value.depthFirst().sum {
									it.@size
								}.messages ?: 0) // Size object
							}
							it
						}
								, new TreeTableColumn('Child sub-tree folders').with {
//										it.setPrefWidth(80);
							it.cellValueFactory = { TreeTableColumn.CellDataFeatures<Node, String> param ->
								new ReadOnlyLongWrapper(param.value.value.depthFirst().size() - 1) // Size object
							}
							it
						}
						);
						// http://stackoverflow.com/questions/10952111/javafx-2-0-table-with-multiline-table-header
						it.columns.forEach { col ->
							Label label = new Label(col.getText());
							label.setStyle('-fx-padding: 0px; -fx-font: 10px "Serif"; -fx-text-alignment: center; -fx-min-width: 4em;'); // http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html
							label.setWrapText(true);

							StackPane stack = new StackPane();
							stack.getChildren().add(label);
							stack.prefWidthProperty().bind(col.widthProperty().subtract(5));
							label.prefWidthProperty().bind(stack.prefWidthProperty());
							col.setGraphic(stack);
						}
						it.columnResizePolicy = it.CONSTRAINED_RESIZE_POLICY
						it
					}
				}
			}
		}
	}
)