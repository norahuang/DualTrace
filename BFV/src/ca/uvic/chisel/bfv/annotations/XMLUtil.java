package ca.uvic.chisel.bfv.annotations;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import ca.uvic.chisel.bfv.annotations.xml.CommentsData;
import ca.uvic.chisel.bfv.annotations.xml.MessageTypesData;
import ca.uvic.chisel.bfv.annotations.xml.RegionsData;
import ca.uvic.chisel.bfv.annotations.xml.TagsData;
import ca.uvic.chisel.bfv.dualtrace.MessageType;
import ca.uvic.chisel.bfv.editor.RegistryUtils;
import ca.uvic.chisel.bfv.utils.IFileUtils;

/**
 * Contains static utility methods for reading and writing file XML metadata such as regions, comments and tags.
 * @author Laura Chan
 */
public class XMLUtil {
	
	static IFileUtils fileUtils = RegistryUtils.getFileUtils();
	
	/**
	 * Write the specified file's comment data to the file's comments file (will first create it if necessary). 
	 * @param storage storage whose comment data needs to be written
	 * @throws JAXBException if something goes wrong while getting a JAXB context or while marshalling to XML
	 * @throws CoreException if something goes wrong when refreshing the comments file in the workspace
	 */
	public static void writeCommentData(FileAnnotationStorage storage) throws JAXBException, CoreException {
		IFile commentsFile = storage.getCommentsFile();
		File commentFile = new File(fileUtils.getCommentFileName(storage.getFile().toString()));
		
		CommentsData commentsData = new CommentsData();
		commentsData.setParentFolder(storage.getParentFolderName());
		commentsData.setAssociatedFile(storage.getFileName());
		commentsData.setCommentGroups(storage.getCommentGroups());
		
		JAXBContext context = JAXBContext.newInstance(CommentsData.class);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.marshal(commentsData, commentFile);
		
		commentsFile.refreshLocal(IResource.DEPTH_ZERO, null);
	}
	
	/**
	 * Read and return comment data from the file's comments file
	 * @param storage storage for file whose comments file is to be read
	 * @return a map of all of the comments read from the comments file, where the comments' names are the keys and the actual comments are the values
	 * @throws JAXBException if something goes wrong while getting a JAXB context or while unmarshalling from XML
	 */
	public static SortedMap<String, CommentGroup> readCommentData(FileAnnotationStorage storage) throws JAXBException {
		File commentsFile = new File(fileUtils.getCommentFileName(storage.getFile().toString()));
		SortedMap<String, CommentGroup> comments = new TreeMap<String, CommentGroup>(new Comparator<String>() {
			@Override
			public int compare(String string1, String string2) {
				// Make the comments with no group appear at the top of the list
				if (CommentGroup.NO_GROUP.equals(string1) && CommentGroup.NO_GROUP.equals(string2)) {
					return 0;
				} else if (CommentGroup.NO_GROUP.equals(string1)) {
					return -1;
				} else if (CommentGroup.NO_GROUP.equals(string2)) {
					return 1;
				} else {
					return string1.compareToIgnoreCase(string2);
				}
			}
		});
		
		if (commentsFile.exists()) {
			JAXBContext context = JAXBContext.newInstance(CommentsData.class);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			CommentsData commentsData = (CommentsData) unmarshaller.unmarshal(commentsFile);
			
			for (CommentGroup group : commentsData.getCommentGroups()) {
				// Storing the comment group in the XML for each comment would probably cause infinitely deep XML, so
				// we'll reconstruct that information here
				for (Comment comment : group.getComments()) {
					comment.setCommentGroup(group);
				}
				comments.put(group.getName(), group);
			}
		}
		return comments;
	}
	
	/**
	 * Write the specified file storage's region data to the its regions file (will first create it if necessary).
	 * @param storage file storage whose region data needs to be written
	 * @throws JAXBException if something goes wrong while getting a JAXB context or while marshalling to XML
	 * @throws CoreException if something goes wrong when refreshing the regions file in the workspace
	 */
	public static void writeRegionData(FileAnnotationStorage storage) throws JAXBException, CoreException {
		IFile regionsFile = storage.getRegionsFile();
		File regionFile = new File(fileUtils.getRegionFileName(storage.getFile().toString()));
		
		RegionsData regionsData = new RegionsData();
		regionsData.setParentFolder(storage.getParentFolderName());
		regionsData.setAssociatedFile(storage.getFileName());
		regionsData.setRegions(storage.getRegions());
		
		JAXBContext context = JAXBContext.newInstance(RegionsData.class);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.marshal(regionsData, regionFile);
		
		regionsFile.refreshLocal(IResource.DEPTH_ZERO, null);
	}
		
	/**
	 * Read and return region data from the file's regions file
	 * @param storage file storage whose regions file is to be read
	 * @return a map of all of the regions read from the regions file, where the regions' names are the keys and the actual regions are the values
	 * @throws JAXBException if something goes wrong while getting a JAXB context or while unmarshalling from XML
	 * @throws IOException 
	 */
	public static SortedMap<Integer, RegionModel> readRegionData(FileAnnotationStorage storage) throws JAXBException {
		File regionsFile = new File(fileUtils.getRegionFileName(storage.getFile().toString()));
		SortedMap<Integer, RegionModel> regions = new TreeMap<Integer, RegionModel>();
		
		if (regionsFile.exists()) {
			JAXBContext context = JAXBContext.newInstance(RegionsData.class);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			RegionsData regionsData = (RegionsData) unmarshaller.unmarshal(regionsFile);
			
			for (RegionModel r : regionsData.getRegions()) {
				// Associate the region and all of its children with the file storage
				r.setAnnotationStorage(storage);
				
				// Trying to store parent region info in the XML was causing issues, so we have to reconstruct it after loading from XML
				reconstructParentInfo(r); 
				regions.put(r.getStartLine(), r);
			}
		}
		return regions;
	}
	
	/**
	 * Helper method to recursively reconstruct the parent region information for the specified region's children after loading from XML
	 * @param parentRegion region whose children's parent info needs to be reconstructed
	 */
	private static void reconstructParentInfo(RegionModel parentRegion) {
		for (RegionModel child : parentRegion.getChildren()) {
			child.setParent(parentRegion);
			reconstructParentInfo(child);
		}
	}
	
	/**
	 * Write the specified file storage's tag data to the its tags file (will first create it if necessary).
	 * @param storage file storage whose tag data needs to be written
	 * @throws JAXBException if something goes wrong while getting a JAXB context or while marshalling to XML
	 * @throws CoreException if something goes wrong when refreshing the tags file in the workspace
	 */
	public static void writeTagData(FileAnnotationStorage storage) throws JAXBException, CoreException {
		IFile tagsFile = storage.getTagsFile();
		File tagFile = new File(fileUtils.getTagFileName(storage.getFile().toString()));
		
		TagsData tagsData = new TagsData();
		tagsData.setParentFolder(storage.getParentFolderName());
		tagsData.setAssociatedFile(storage.getFileName());
		tagsData.setTags(storage.getTags());
		
		JAXBContext context = JAXBContext.newInstance(TagsData.class);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.marshal(tagsData, tagFile);
		
		tagsFile.refreshLocal(IResource.DEPTH_ZERO, null);
	}
	
	/**
	 * Read and return tag data from the file's tags file
	 * @param storage file storage whose tags file is to be read
	 * @return a map of all of the tags read from the tags file, where the tags' names are the keys and the actual tags are the values
	 * @throws JAXBException if something goes wrong while getting a JAXB context or while unmarshalling from XML
	 */
	public static SortedMap<String, Tag> readTagData(FileAnnotationStorage storage) throws JAXBException {
		File tagsFile = new File(fileUtils.getTagFileName(storage.getFile().toString()));
		SortedMap<String, Tag> tags = new TreeMap<String, Tag>(new Comparator<String>() {
			@Override
			public int compare(String string1, String string2) {
				return string1.compareToIgnoreCase(string2);
			}
		});
		
		if (tagsFile.exists()) {
			JAXBContext context = JAXBContext.newInstance(TagsData.class);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			TagsData tagsData = (TagsData) unmarshaller.unmarshal(tagsFile);
			
			for (Tag tag : tagsData.getTags()) {
				// Storing the associated tag in the XML for each tag occurrence would probably cause infinitely deep XML, so
				// we'll reconstruct that information here
				for (TagOccurrence occurrence : tag.getOccurrences()) {
					occurrence.setTag(tag);
				}
				tags.put(tag.getName(), tag);
			}
		}
		return tags;
	}

	public static SortedMap<String, MessageType> readMessageTypeData(FileAnnotationStorage storage) throws JAXBException {
		File messageTypeFile = new File(fileUtils.getMessageTypeFileName(storage.getFile().getParent().toString()));
		SortedMap<String, MessageType> types = new TreeMap<String, MessageType>(new Comparator<String>() {
			@Override
			public int compare(String string1, String string2) {
				return string1.compareToIgnoreCase(string2);
			}
		});
		
		if (messageTypeFile.exists()) {
			JAXBContext context = JAXBContext.newInstance(MessageTypesData.class);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			MessageTypesData typesData = (MessageTypesData) unmarshaller.unmarshal(messageTypeFile);
			
			for (MessageType type : typesData.getMessageTypes()) {
				types.put(type.getName(), type);
			}
			}
			return types;
	}
	
	public static void writeMessageTypeData(FileAnnotationStorage storage) throws JAXBException, CoreException {
		IFile messageTypesFile = storage.getMessageTypesFile();
		File messageTypeFile = new File(fileUtils.getMessageTypeFileName(storage.getFile().getParent().toString()));
		
		MessageTypesData messageTypesData = new MessageTypesData();
		messageTypesData.setParentFolder(storage.getParentFolderName());
		messageTypesData.setMessageTypes(storage.getMessageTypes());
		
		JAXBContext context = JAXBContext.newInstance(MessageTypesData.class);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.marshal(messageTypesData, messageTypeFile);
		
		messageTypesFile.refreshLocal(IResource.DEPTH_ZERO, null);
	}
	
}
