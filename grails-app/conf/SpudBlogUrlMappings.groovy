
class SpudBlogUrlMappings {

	static mappings = { appContext ->

		def spudBlogService = appContext.spudBlogService
		def blogMapping = spudBlogService.blogMapping
		def newsMapping = spudBlogService.newsMapping
		def newsEnabled = spudBlogService.isNewsEnabled()
		def blogEnabled = spudBlogService.isBlogEnabled()

		//
		if(blogEnabled) {
			def BLOG_MAPPING = "/${blogMapping}"
			invokeMethod(BLOG_MAPPING + ".${format}",[resources: 'blog'])
			invokeMethod(BLOG_MAPPING,[resources: 'blog'])
			"/blog/latestPost"(controller: 'blog', action: 'latestPost')
			"/blog/index"(controller: 'blog', action: 'index')
	        "/spud/admin/blog-posts"(resources: 'blogPosts', namespace: 'spud_admin')
		}
		if(newsEnabled) {

			def NEWS_MAPPING = "/${newsMapping}"
			invokeMethod(NEWS_MAPPING,[resources: 'news'])
			invokeMethod(NEWS_MAPPING + ".${format}",[resources: 'news'])
			"/spud/admin/news-posts"(resources: 'newsPosts', namespace: 'spud_admin')
		}


	}
}
