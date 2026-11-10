package es.burl.cms.controllers.editor;

/** Thrown by editor controllers for a page, gallery or exhibition that doesn't exist; shown as the 404 page. */
public class NotFoundException extends RuntimeException {

	public NotFoundException(String what) {
		super(what + " was not found");
	}
}
