package spud.admin
import  spud.core.*
import  spud.security.*
import  spud.blog.*

class PostsController {

	def sitemapService
	def spudPageService
	def spudBlogService
	def spudMultiSiteService
	def sharedSecurityService

	static allowedMethods = [update:['GET', 'DELETE', 'PUT', 'POST'], updatePostThumbnail: ['POST']]

	def index() {

		//def siteIds = spudMultiSiteService.availableSites().collect { it.siteId }
		def posts
		def postCount
		postCount = SpudPost.executeQuery("select count(p) from SpudPost p WHERE p.isNews = :isNews)",[isNews: news()])[0]
		posts = SpudPost.findAll("from SpudPost p WHERE p.isNews = :isNews ORDER BY publishedAt desc",[isNews: news()],[max:25] + params)
		// log.debug "Checking Active Site ${spudMultiSiteService.activeSite.siteId}"
		// if(spudMultiSiteService.activeSite.siteId == 0) {
		// 	postCount = SpudPost.executeQuery("select count(p) from SpudPost p WHERE p.isNews = :isNews AND ( NOT EXISTS ( FROM SpudPostSite s WHERE s.post = p) OR EXISTS ( FROM SpudPostSite s Where s.post = p AND s.spudSiteId = :siteId))",[isNews: news(), siteId: spudMultiSiteService.activeSite.siteId])[0]
		// 	posts = SpudPost.findAll("from SpudPost p WHERE p.isNews = :isNews AND ( NOT EXISTS ( FROM SpudPostSite s WHERE s.post = p) OR EXISTS ( FROM SpudPostSite s Where s.post = p AND s.spudSiteId = :siteId)) ORDER BY publishedAt desc",[isNews: news(), siteId: spudMultiSiteService.activeSite.siteId],[max:25] + params)
		// } else {
		// 	postCount = SpudPost.executeQuery("select count(p) from SpudPost p WHERE p.isNews = :isNews AND EXISTS ( FROM SpudPostSite s Where s.post = p AND s.spudSiteId = :siteId)",[isNews: news(), siteId: spudMultiSiteService.activeSite.siteId])[0]
		// 	posts = SpudPost.findAll("from SpudPost p WHERE p.isNews = :isNews AND EXISTS ( FROM SpudPostSite s Where s.post = p AND s.spudSiteId = :siteId) ORDER BY publishedAt desc",[isNews: news(), siteId: spudMultiSiteService.activeSite.siteId],[max:25] + params)

		// }

		render view: '/spud/admin/posts/index', model: [posts: posts, postCount: postCount ]
	}

	def show() {

	}

	def create() {
		def post = new SpudPost(isNews: news(), publishedAt: new Date(), userId: sharedSecurityService.getUserIdentity())
		render view: '/spud/admin/posts/create', model: [post: post]
	}


	def save() {

		if(!params.post) {
			flash.error = "Post submission not specified"
			redirect action: 'index', method: 'GET', namespace: 'spud_admin'
			return
		}

		def imageMap = [:]
		def imageContentType = params.thumbnailImage?.contentType
		if (imageContentType == null || !(imageContentType =~ "image/")) {
			imageMap.put("thumbnailImage", null)
			imageMap.put("thumbnailImageName", null)
			imageMap.put("thumbnailImageContentType", null)
		} else {
			imageMap.put("thumbnailImage", params.thumbnailImage?.bytes)
			imageMap.put("thumbnailImageName", params.thumbnailImage?.originalFilename)
			imageMap.put("thumbnailImageContentType", params.thumbnailImage?.contentType)
		}

		def newPostParams = params.post + imageMap

		def post = new SpudPost(newPostParams)

		post.isNews = news()
		post.userId = sharedSecurityService.getUserIdentity()
		spudBlogService.generateUrlName(post)
		def sites = params.list('sites') ?: []

		// if(!sites) {
		// 	sites << spudMultiSiteService.activeSite.siteId
		// }
		// sites?.each { site ->
		// 	post.addToSites(new SpudPostSite(spudSiteId: site.toInteger()))
		// }

		def selectedCategories = params.categories
		if(selectedCategories){
			List<String> categories = selectedCategories?.trim()?.tokenize(",")
			categories?.each{ cat ->
				SpudPostCategory c = SpudPostCategory.findOrCreateByName(cat)
				if(!c.id){
					c.name =  cat
					c.urlName = java.net.URLEncoder.encode(cat, "UTF-8")
				}
				post.addToCategories(c)
			}
		}
		if(post.save(flush:true)) {
			sitemapService.evictCache()
			spudPageService?.evictCache()
			redirect action: 'index', method: 'GET', namespace: 'spud_admin'
		} else {
			println post.errors
			flash.error = "Error Saving Post"
			render view: '/spud/admin/posts/create', model: [post: post]
		}

	}


	def edit() {
		def post = loadPost()
		if(!post) {
			return
		}
		render view: '/spud/admin/posts/edit', model: [post: post]
	}


	def update() {
		def post = loadPost()
		if(!post) {
			return
		}

		bindData(post, params.post)
		post.isNews = news()
		spudBlogService.generateUrlName(post)
		// def sites = params.list('sites') ?: []
		// post.sites.clear()
		// if(!sites) {
		// 	sites << spudMultiSiteService.activeSite.siteId
		// }
		// sites.each { site ->
		// 	post.addToSites(new SpudPostSite(spudSiteId: site.toInteger()))
		// }
		post.categories = null
		if(post.save(flush:true)) {
			def selectedCategories = params.categories
			if(selectedCategories){
				List<String> categories = selectedCategories?.replaceAll("\\s", "")?.tokenize(",")
				categories?.each{ cat ->
					SpudPostCategory c = SpudPostCategory.findOrCreateByName(cat)
					if(!c.id){
						c.name =  cat
						c.urlName = java.net.URLEncoder.encode(cat, "UTF-8")
					}
					post.addToCategories(c)
				}
			}
			post.save(flush:true)
			sitemapService.evictCache()
			spudPageService?.evictCache()
			redirect action: 'index', method: 'GET', namespace: 'spud_admin'
		} else {
			log.error post.errors
			flash.error = "Error Saving Post"
			render view: '/spud/admin/posts/edit', model: [post: post]
		}
	}

	// THIS ACTION WAS CREATED BECAUSE UPDATE METHOD ONLY ALLOWS
	// PUT REQUESTS, SO IT'S NOT POSSIBLE TO PASS THE NEW
	// THUMBNAIL WITHIN THE PUT REQUEST
	def updatePostThumbnail(){
		def post = loadPost()
		if(!post) {
			return
		}
		def imageMap = [:]
		def imageContentType = params.thumbnailImage?.contentType
		if (imageContentType == null || !(imageContentType =~ "image/")) {
			imageMap.put("thumbnailImage", null)
			imageMap.put("thumbnailImageName", null)
			imageMap.put("thumbnailImageContentType", null)
		} else {
			imageMap.put("thumbnailImage", params.thumbnailImage?.bytes)
			imageMap.put("thumbnailImageName", params.thumbnailImage?.originalFilename)
			imageMap.put("thumbnailImageContentType", params.thumbnailImage?.contentType)
		}
		bindData(post, imageMap)
		if(post.save(flush:true)){
			flash.message = "Post Thumnail updated!"
		}else{
			flash.error = "There was an error updating the thumbnail ${post.errors}"
		}
		render view: '/spud/admin/posts/edit', model: [post: post]
	}


	def delete() {
		def post = loadPost()
		if(!post) {
			return
		}
		spudPageService?.evictCache()
		sitemapService.evictCache()
		post.delete()
		redirect action: 'index', method: 'GET', namespace: 'spud_admin'
	}

	protected loadPost() {
		if(!params.id) {
			flash.error = "Post not specified"
			redirect action: 'index', method: 'GET', namespace: 'spud_admin'
			return null
		}

		def post = SpudPost.find("from SpudPost p WHERE p.id = :id AND p.isNews = :isNews ",[id: params.long('id'), isNews: news()])
		if(!post) {
			flash.error = "Post not found!"
			redirect action: 'index', method: 'GET', namespace: 'spud_admin'
			return null
		}
		return post
	}

	protected news() {
		return false
	}

	def getCategories(){
		def categories = SpudPostCategory.executeQuery("SELECT new map(c.name as name, c.id as id, c.urlName as urlName) \
			FROM SpudPostCategory c WHERE lower(c.name) like :term",
			[term: "%${params.term?.toLowerCase()}%"], [max: 10])

		render(contentType: "application/json") {
			data: categories
		}
	}
}
