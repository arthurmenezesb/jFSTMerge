package br.ufpe.cin.mergers.handlers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;

import br.ufpe.cin.files.FilesManager;
import br.ufpe.cin.mergers.util.MergeConflict;
import br.ufpe.cin.mergers.util.MergeContext;
import br.ufpe.cin.printers.Prettyprinter;
import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.cide.fstgen.ast.FSTTerminal;

/**
 * Renaming or deletions conflicts happen when one developer edits a element renamed or deleted by other.
 * Semistructured merge is unable to detect such cases because it matches elements via its identifier, so
 * if a element is renamed or deleted it cannot match the elements anymore. This class overcomes this issue.
 * @author Guilherme
 *
 */
public final class RenamingOrDeletionConflictsHandler {
	
	public static void handle(MergeContext context) {
		boolean hasRenameConflict = false;
		
		//possible renamings or deletions in left
		if(!context.possibleRenamedLeftNodes.isEmpty() || !context.possibleRenamedRightNodes.isEmpty()){
			for(Pair<String,FSTNode> tuple: context.possibleRenamedLeftNodes){
				List<Pair<Double,String>> similarNodes = new ArrayList<Pair<Double,String>>(); //list of possible nodes renaming a previous one
				if(nodeHasConflict(tuple.getRight()) && isValidNode(tuple.getRight())){
					String baseContent = tuple.getLeft();
					String currentNodeContent= ((FSTTerminal) tuple.getRight()).getBody(); //node content with conflict
					String editedNodeContent = FilesManager.extractMergeConflicts(currentNodeContent).get(0).right;

					//1. getting similar nodes to fulfill renaming conflicts
					for(FSTNode newNode : context.addedLeftNodes){ // a possible renamed node is seem as "new" node due to superimposition
						if(isValidNode(newNode)){
							String possibleRenamingContent = ((FSTTerminal) newNode).getBody();
							double similarity  	  		   = FilesManager.computeStringSimilarity(baseContent, possibleRenamingContent);
							if(similarity >= 0.7){ //a typical value of 0.7 (up to 1.0) is used, increase it for a more accurate comparison, or decrease for a more relaxed one.
								Pair<Double,String> tp = Pair.of(similarity, possibleRenamingContent);
								similarNodes.add(tp);
							}
						}
					}

					//2. checking if unstructured merge also reported the renaming conflict
					String signature = getSignature(baseContent);
					List<MergeConflict> unstructuredMergeConflictsHavingRenamedSignature = FilesManager.extractMergeConflicts(context.unstructuredOutput).stream()
							.filter(mc -> FilesManager.getStringContentIntoSingleLineNoSpacing(mc.body).contains(signature))
							.collect(Collectors.toList());
					if(unstructuredMergeConflictsHavingRenamedSignature.size() > 0){
						//String possibleRenamingContent = getMostSimilarContent(similarNodes);
						//generateRenamingConflict(context, currentNodeContent, possibleRenamingContent, editedNodeContent,false);
						((FSTTerminal) tuple.getRight()).setBody(editedNodeContent);
						hasRenameConflict = true;
					} else { //do not report the renaming conflict
						((FSTTerminal) tuple.getRight()).setBody(editedNodeContent);
					}
				}
			}

			//possible renamings or deletions in right
			for(Pair<String,FSTNode> tuple: context.possibleRenamedRightNodes){
				List<Pair<Double,String>> similarNodes = new ArrayList<Pair<Double,String>>(); //list of possible nodes renaming a previous one
				if(nodeHasConflict(tuple.getRight()) && isValidNode(tuple.getRight())){
					String baseContent = tuple.getLeft();
					String currentNodeContent= ((FSTTerminal) tuple.getRight()).getBody(); //node content with conflict
					String editedNodeContent = FilesManager.extractMergeConflicts(currentNodeContent).get(0).left;

					for(FSTNode newNode : context.addedRightNodes){ // a possible renamed node is seem as "new" node due to superimposition
						if(isValidNode(newNode)){
							String possibleRenamingContent = ((FSTTerminal) newNode).getBody();
							double similarity  	  		   = FilesManager.computeStringSimilarity(baseContent, possibleRenamingContent);
							if(similarity >= 0.7){ //a typical value of 0.7 (up to 1.0) is used, increase it for a more accurate comparison, or decrease for a more relaxed one.
								Pair<Double,String> tp = Pair.of(similarity, possibleRenamingContent);
								similarNodes.add(tp);
							}
						}
					}

					String signature = getSignature(baseContent);
					List<MergeConflict> unstructuredMergeConflictsHavingRenamedSignature = FilesManager.extractMergeConflicts(context.unstructuredOutput).stream()
							.filter(mc -> FilesManager.getStringContentIntoSingleLineNoSpacing(mc.body).contains(signature))
							.collect(Collectors.toList());
					if(unstructuredMergeConflictsHavingRenamedSignature.size() > 0){
						//String possibleRenamingContent = getMostSimilarContent(similarNodes);
						//generateRenamingConflict(context, currentNodeContent, possibleRenamingContent, editedNodeContent,false);
						((FSTTerminal) tuple.getRight()).setBody(editedNodeContent);
						hasRenameConflict = true;
					} else { //do not report the renaming conflict
						((FSTTerminal) tuple.getRight()).setBody(editedNodeContent);
					}
				}
			}
		}
		if(hasRenameConflict) {
			try {
				logMerge(context);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static String getSignature(String source) {
		String trim = FilesManager.getStringContentIntoSingleLineNoSpacing(source);
		String signatureTrimmed = trim.substring(0, (/*is interface?*/(trim.contains("{")) ? trim.indexOf("{") : trim.indexOf(";")));
		return signatureTrimmed;
	}

	private static String getMostSimilarContent(List<Pair<Double, String>> similarNodes) {
		if(!similarNodes.isEmpty()){
			similarNodes.sort((n1, n2) -> n1.getLeft().compareTo(n2.getLeft()));		
			return (similarNodes.get(similarNodes.size()-1)).getRight();// the top of the list
		} else {
			return "";
		}
	}

	private static boolean nodeHasConflict(FSTNode node) {
		if(isValidNode(node)){
			String body = ((FSTTerminal) node).getBody();
			return body.contains("<<<<<<< MINE");
		}
		return false;
	}

	private static boolean isValidNode(FSTNode node) {
		if(node instanceof FSTTerminal){
			String nodeType = ((FSTTerminal)node).getType();
			if(nodeType.equals("MethodDecl") || nodeType.equals("ConstructorDecl")){
				return true;
			}
		}
		return false;
	}

	private static void generateRenamingConflict(MergeContext context,String currentNodeContent, String firstContent,String secondContent, boolean isLeftToRight) {
		
		if(!isLeftToRight){//managing the origin of the changes in the conflict
			String aux 	 = secondContent;
			secondContent= firstContent;
			firstContent = aux;
		}
		
		//statistics
		if(firstContent.isEmpty() || secondContent.isEmpty()){
			context.deletionConflicts++;
		} else {
			context.renamingConflicts++;
		}

		//first creates a conflict 
		MergeConflict newConflict = new MergeConflict(firstContent+'\n', secondContent+'\n');
		
		
		//second put the conflict in one of the nodes containing the previous conflict, and deletes the other node containing the possible renamed version
		FilesManager.findAndReplaceASTNodeContent(context.superImposedTree, currentNodeContent, newConflict.body);
		if(isLeftToRight){
			FilesManager.findAndDeleteASTNode(context.superImposedTree, firstContent);
		} else {
			FilesManager.findAndDeleteASTNode(context.superImposedTree, secondContent);

		}
	}

		//pure similarity-based handler (it works)

 	/* public static void handle(MergeContext context) {
		//possible renamings or deletions in left
		if(!context.possibleRenamedLeftNodes.isEmpty() || !context.possibleRenamedRightNodes.isEmpty()){
			for(Pair<String,FSTNode> tuple: context.possibleRenamedLeftNodes){
				List<Pair<Double,String>> similarNodes = new ArrayList<Pair<Double,String>>(); //list of possible nodes renaming a previous one
				if(nodeHasConflict(tuple.getRight())){
					String baseContent = tuple.getLeft();
					String currentNodeContent= ((FSTTerminal) tuple.getRight()).getBody(); //node content with conflict
					String editedNodeContent = FilesManager.extractMergeConflicts(currentNodeContent).get(0).right;
					for(FSTNode newNode : context.addedLeftNodes){ // a possible renamed node is seem as "new" node due to superimposition
						if(isValidNode(newNode)){
							String possibleRenamingContent = ((FSTTerminal) newNode).getBody();
							double similarity  	  		   = FilesManager.computeStringSimilarity(baseContent, possibleRenamingContent);
							if(similarity >= 0.7){ //a typical value of 0.7 (up to 1.0) is used, increase it for a more accurate comparison, or decrease for a more relaxed one.
								Pair<Double,String> tp = Pair.of(similarity, possibleRenamingContent);
								similarNodes.add(tp);
							}
						}
					}
					if(similarNodes.isEmpty()){//there is no similar node. it is a possible deletion, so remove the conflict keeping the edited version of the content 
						FilesManager.findAndReplaceASTNodeContent(context.superImposedTree, currentNodeContent,editedNodeContent);

						//statistics
						context.deletionConflicts++;
					} else {
						String possibleRenamingContent = getMostSimilarContent(similarNodes);
						generateRenamingConflict(context, currentNodeContent, possibleRenamingContent, editedNodeContent,true);
					}
				}
			}

			//possible renamings or deletions in right
			for(Pair<String,FSTNode> tuple: context.possibleRenamedRightNodes){
				List<Pair<Double,String>> similarNodes = new ArrayList<Pair<Double,String>>(); //list of possible nodes renaming a previous one
				if(nodeHasConflict(tuple.getRight())){
					String baseContent = tuple.getLeft();
					String currentNodeContent= ((FSTTerminal) tuple.getRight()).getBody(); //node content with conflict
					String editedNodeContent = FilesManager.extractMergeConflicts(currentNodeContent).get(0).left;
					for(FSTNode newNode : context.addedRightNodes){ // a possible renamed node is seem as "new" node due to superimposition
						if(isValidNode(newNode)){
							String possibleRenamingContent = ((FSTTerminal) newNode).getBody();
							double similarity  	  		   = FilesManager.computeStringSimilarity(baseContent, possibleRenamingContent);
							if(similarity >= 0.7){ //a typical value of 0.7 (up to 1.0) is used, increase it for a more accurate comparison, or decrease for a more relaxed one.
								Pair<Double,String> tp = Pair.of(similarity, possibleRenamingContent);
								similarNodes.add(tp);
							}
						}
					}
					if(similarNodes.isEmpty()){//there is no similar node. it is a possible deletion, so remove the conflict keeping the edited version of the content 
						FilesManager.findAndReplaceASTNodeContent(context.superImposedTree, currentNodeContent,editedNodeContent);

						//statistics
						context.deletionConflicts++;
					} else {
						String possibleRenamingContent = getMostSimilarContent(similarNodes);
						generateRenamingConflict(context, currentNodeContent, possibleRenamingContent, editedNodeContent,false);
					}
				}
			}
		}
	} */
	
	private static void logMerge(MergeContext context) throws IOException { 
		String logpathJfstmerge = System.getProperty("user.home")+ File.separator + ".jfstmerge" + File.separator;
		new File(logpathJfstmerge).mkdirs(); //ensuring that the directories exists
		
		//remove dot (.) of filename
		String filenameBase = context.getBase().getName().substring(1);
		String filenameLeft = context.getLeft().getName().substring(1);
		String filenameRight = context.getRight().getName().substring(1);
		
		String logpath = logpathJfstmerge + File.separator + "results" + File.separator + filenameBase + File.separator;
		new File(logpath).mkdirs(); //ensuring that the directories exists
		
		String logpathRenameMerge = logpath + filenameBase + "-rename.merge";
		String logpathBase = logpath + filenameBase + "-rename.base";
		String logpathLeft = logpath + filenameLeft + "-rename.left";
		String logpathRight = logpath + filenameRight + "-rename.right";
		
		//MERGE
		File fileLogRenameMerge = new File(logpathRenameMerge);
		if(!fileLogRenameMerge.exists()){
			FileUtils.write(fileLogRenameMerge, "", true);
		}
		
		String printMerge = FilesManager.indentCode(Prettyprinter.print(context.superImposedTree));
		//printMerge = printMerge.replace("<<<<<<< MINE", "");
		//printMerge = printMerge.replace("======= MINE", "");
		//printMerge = printMerge.replace(">>>>>>> YOURS", "");
		
		FileUtils.write(fileLogRenameMerge, printMerge, true);
		
		//BASE
		File fileLogBase = new File(logpathBase);
		if(!fileLogBase.exists()){
			FileUtils.write(fileLogBase, "", true);
		}
		
		FileUtils.write(fileLogBase, FilesManager.indentCode(Prettyprinter.print(context.baseTree)), true);
		
		//LEFT
		File fileLogLeft = new File(logpathLeft);
		if(!fileLogLeft.exists()){
			FileUtils.write(fileLogLeft, "", true);
		}
		
		FileUtils.write(fileLogLeft, FilesManager.indentCode(Prettyprinter.print(context.leftTree)), true);
		
		//RIGHT
		File fileLogRight = new File(logpathRight);
		if(!fileLogRight.exists()){
			FileUtils.write(fileLogRight, "", true);
		}
		
		FileUtils.write(fileLogRight, FilesManager.indentCode(Prettyprinter.print(context.rightTree)), true);	
	}
	
}
