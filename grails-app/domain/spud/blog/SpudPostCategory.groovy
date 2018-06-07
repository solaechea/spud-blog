package spud.blog

class SpudPostCategory {
	String name
	String urlName
	SpudPostCategory parent

    static belongsTo = SpudPost
    static hasMany = [posts: SpudPost]

	static mapping = {
		def cfg = it?.getBean('grailsApplication')?.config
		datasource(cfg?.spud?.core?.datasource ?: 'DEFAULT')
		table 'spud_post_categories'
        posts joinTable: [name: "spud_post_categories_spud_posts", key: 'mm_category_id' ]
	}

	static constraints = {
		name nullable:false, unique: 'parent'
		urlName nullable:false
		parent nullable:true
	}

	def grailsCacheAdminService

	def afterInsert() {
		grailsCacheAdminService.clearAllCaches()
	}

	def afterUpdate() {
		grailsCacheAdminService.clearAllCaches()
	}

	def afterDelete() {
		grailsCacheAdminService.clearAllCaches()
	}
}
