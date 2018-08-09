import spud.blog.*
import grails.converters.*

class SpudBlogBootStrap {
	def init = { servletContext ->
		JSON.registerObjectMarshaller(SpudPost) {
			[
				id              : it.id,
				title           : it.title,
				urlName         : it.urlName,
				content         : it.render(),
				publishedAt     : it.publishedAt,
				dateCreated     : it.dateCreated,
				lastUpdated     : it.lastUpdated,
				userId          : it.userId,
				userDisplayName : it.userDisplayName
			]
		}
	}

	def destroy = {

	}
}
