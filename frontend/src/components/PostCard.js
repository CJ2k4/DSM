function PostCard({ post }) {
    return (
      <div className="card">
        <h4>{post.username}</h4>
        <p>{post.content}</p>
        <small>{new Date(post.timestamp).toLocaleString()}</small>
      </div>
    );
  }
  
  export default PostCard;