package spud.blog

import grails.converters.*
import groovy.time.TimeCategory
import org.codehaus.groovy.grails.web.mapping.LinkGenerator

class BlogController {
	def grailsApplication
	def spudMultiSiteService
	LinkGenerator grailsLinkGenerator
	static responseFormats = ['html','rss','atom']

    def index() {
    	def postsPerPage = grailsApplication.config.spud.blog.postsPerPage ?: 25
    	def layout = grailsApplication.config.spud.blog.blogLayout ?: 'main'
		def siteId = params.int('siteId') ?: request.getAttribute('spudSiteId')
		def today  = new Date()
		def postQuery
		def postCount
		if(siteId == 0) {
			postCount = SpudPost.executeQuery("select count(p) from SpudPost p WHERE p.isNews = false AND visible=true AND publishedAt <= :today AND ( NOT EXISTS ( FROM SpudPostSite s WHERE s.post = p) OR EXISTS ( FROM SpudPostSite s Where s.post = p AND s.spudSiteId = :siteId))",[siteId: siteId, today: today])[0]
			postQuery = "from SpudPost p WHERE p.isNews = false AND visible=true AND publishedAt <= :today AND ( NOT EXISTS ( FROM SpudPostSite s WHERE s.post = p) OR EXISTS ( FROM SpudPostSite s Where s.post = p AND s.spudSiteId = :siteId)) ORDER BY publishedAt desc"
		} else {
			postCount = SpudPost.executeQuery("select count(p) from SpudPost p WHERE p.isNews = false AND visible=true AND publishedAt <= :today AND EXISTS ( FROM SpudPostSite s Where s.post = p AND s.spudSiteId = :siteId)",[ siteId: siteId, today: today])[0]
			postQuery = "from SpudPost p WHERE p.isNews = false AND visible=true AND publishedAt <= :today AND EXISTS ( FROM SpudPostSite s Where s.post = p AND s.spudSiteId = :siteId) ORDER BY publishedAt desc"

		}
		withFormat {
			html {
				def posts = SpudPost.findAll(postQuery,[siteId:siteId, today:today], [max:postsPerPage] + params)
				def recentPosts = SpudPost.executeQuery("SELECT new map(p.id as id, p.title as title, p.urlName as urlName) FROM SpudPost p WHERE p.isNews = false \
					AND visible=true AND publishedAt <= :today ORDER BY publishedAt desc", [today:today], [max: 5])
				render view: '/blog/index', model: [posts: posts, postCount: postCount, layout: layout, recentPosts: recentPosts]
			}
			rss {
				render(feedType:"rss", feedVersion:"2.0") {
					title = grailsApplication.config.spud.siteName ?: grailsApplication.config.spud.blog.blogName ?: 'Spud Blog'

					link = g.createLink(controller:'blog',action:'index',absolute:true)

					description = grailsApplication.config.spud.blog.blogDescription ?: 'Spud blog Description'
					def posts = SpudPost.findAll(postQuery,[siteId:siteId, today:today])
					posts.each { post ->
						entry(post.title) {
							link = g.createLink(controller:'blog',action:'show', id: post.urlName,absolute:true)
							post.contentProcessed
						}
					}
				}

			}
			atom {
				render(feedType:"atom") {
					title = grailsApplication.config.spud.siteName ?: grailsApplication.config.spud.blog.blogName ?: 'Spud Blog'
					link = g.createLink(controller:'blog',action:'index',absolute:true)
					description = grailsApplication.config.spud.blog.blogDescription ?: 'Spud blog Description'
					def posts = SpudPost.findAll(postQuery,[siteId:siteId, today:today])

					posts.each { post ->
						entry(post.title) {
							link = g.createLink(controller:'blog',action:'show', id: post.urlName,absolute:true)
							post.contentProcessed
						}
					}
				}
			}
			json {
				def posts = SpudPost.findAll(postQuery,[siteId:siteId, today:today], [max:postsPerPage] + params)
				render([posts: posts, postCount: postCount] as JSON)
			}
			xml {
				def posts = SpudPost.findAll(postQuery,[siteId:siteId, today:today], [max:postsPerPage] + params)
				render([posts: posts, postCount: postCount] as XML)
			}

		}
    }

    def search() {
    	def postsPerPage = grailsApplication.config.spud.blog.postsPerPage ?: 25
    	def layout = grailsApplication.config.spud.blog.blogLayout ?: 'main'
    	Date today = new Date()
    	String query = params.q?:""
    	query = query.trim().toLowerCase()

		def postCount = SpudPost.executeQuery("select count(p) from SpudPost p WHERE p.isNews = false \
			AND visible=true AND publishedAt <= :today AND (lower(p.title) like :q \
			OR lower(p.metaDescription) like :q)",[today: today, q: "%${query}%"])[0]
		def postQuery = "from SpudPost p WHERE p.isNews = false AND visible=true AND publishedAt <= :today \
			AND (lower(p.metaDescription) like :q OR lower(p.title) like :q) \
			ORDER BY publishedAt desc"

		def recentPosts = SpudPost.executeQuery("SELECT new map(p.id as id, p.title as title, p.urlName as urlName) FROM SpudPost p WHERE p.isNews = false \
			AND visible=true AND publishedAt <= :today ORDER BY publishedAt desc", [today:today], [max: 5])

		def posts = SpudPost.findAll(postQuery, [today: today, q: "%${query}%"], [max:postsPerPage] + params)
		render view: '/blog/index', model: [posts: posts, postCount: postCount, layout: layout, q: params.q, recentPosts: recentPosts]
    }


    def show() {
    	def post = SpudPost.where { isNews == false && visible == true && publishedAt <= new Date() && urlName == params.id}.find()
    	def layout = grailsApplication.config.spud.blog.blogLayout ?: 'main'
    	def today  = new Date()
		def recentPosts = SpudPost.executeQuery("SELECT new map(p.id as id, p.title as title, p.urlName as urlName) FROM SpudPost p WHERE p.isNews = false \
			AND visible=true AND publishedAt <= :today ORDER BY publishedAt desc", [today:today], [max: 5])

    	if(!post) {
    		redirect action: 'index'
    		return
    	}

		def nextPost, previousPost
		def nextPostResult = SpudPost.executeQuery("SELECT new map(p.id as id, p.title as title, p.urlName as urlName) FROM SpudPost p WHERE p.isNews = false \
			AND visible=true AND publishedAt <= :today AND publishedAt > :currentPostDate \
			AND p.id != :currentPostId ORDER BY publishedAt desc",
			[today:today, currentPostDate: post.publishedAt, currentPostId: post.id], [max: 1])

		def previousPostResult = SpudPost.executeQuery("SELECT new map(p.id as id, p.title as title, p.urlName as urlName) FROM SpudPost p WHERE p.isNews = false \
			AND visible=true AND publishedAt <= :today AND publishedAt <= :currentPostDate \
			AND p.id != :currentPostId ORDER BY publishedAt desc",
			[today:today, currentPostDate: post.publishedAt, currentPostId: post.id], [max: 1])

		if(nextPostResult){
			nextPost = nextPostResult[0]
		}
		if(previousPostResult){
			previousPost = previousPostResult[0]
		}
		withFormat {
			html {
				render view: '/blog/show', model: [post: post, layout: layout, recentPosts: recentPosts,
					nextPost: nextPost, previousPost: previousPost]
			}
			json {
				render post as JSON
			}
			xml {
				render post as XML
			}
		}


    }

	def latestPost(){
		def yesterday
		use (TimeCategory) {
			yesterday = new Date() - params.int('hours')?.hours
		}
		def post = SpudPost.find("from SpudPost p WHERE p.isNews = :isNews and p.publishedAt > :yesterday and p.publishedAt < :now ORDER BY publishedAt desc",[isNews: false,yesterday: yesterday, now:new Date() ])
		def jsonData = [:]
		if (post){
			jsonData.'thumbnailImage'= post.thumbnailImage?.encodeBase64().toString()
			jsonData.'thumbnailImageName' = post?.thumbnailImageName
			jsonData.'thumbnailImageContentType' = post?.thumbnailImageContentType
			jsonData.'title' = post?.title
			jsonData.'urlName' = grailsLinkGenerator.link(resource:"blog", action:"show", id:"${post?.urlName}", absolute: true)
			jsonData.'content' = post.content
			jsonData.'publishedAt'= post.publishedAt
		}
		render text: jsonData as JSON, contentType: 'application/json', status: 200
	}

}
